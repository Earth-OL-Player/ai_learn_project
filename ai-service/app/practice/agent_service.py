from __future__ import annotations

import json
import logging
import re
import time
import uuid
from collections.abc import Iterator
from typing import Any

from langchain.agents import create_agent
from langchain.chat_models import init_chat_model
from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, SystemMessage
from pydantic import BaseModel

from app.config.settings import settings
from app.schemas.practice import (
    PracticeDiscussRequest,
    PracticeDiscussResponse,
    PracticeGradeRequest,
    PracticeGradeResponse,
)

_SPLIT_PATTERN = re.compile(r"[\s，。、；：,.!?！？（）()\"'“”‘’]+")
_MAX_KEYWORDS = 10
_LOCAL_RULE_MODEL = "LOCAL_RULE"
_API_KEY_PLACEHOLDER = "AI_GRADING_API_KEY占位符"
_CHAT_COMPLETIONS_PATH = "/chat/completions"
_FALLBACK_DISCUSS_REPLY = "抱歉，当前大模型调用异常，仅保留兜底策略评分功能，无法和您进行探讨。"
_GRADE_SYSTEM_PROMPT = "你是严谨的 AI 学习助手，请使用简体中文完成面试题评分。"
_DISCUSSION_SYSTEM_PROMPT = "你是严谨的 AI 学习助手，请只围绕当前刷题上下文回答，使用简体中文。"
_DEEPSEEK_THINKING_DISABLED_BODY = {"thinking": {"type": "disabled"}}
_GRADE_JSON_INSTRUCTION = (
    "请只输出一个合法 JSON 对象，不要输出 Markdown、代码块或额外解释。"
    "JSON 字段必须包含 score、correct、hitPoints、missingPoints、problems、"
    "referenceAnswer、improvementAdvice、reviewKnowledgePoints、fallbackUsed。"
    "referenceAnswer 必须使用参考答案原文，fallbackUsed 固定为 false。"
    "示例 JSON：{\"score\":0,\"correct\":false,\"hitPoints\":[],\"missingPoints\":[],"
    "\"problems\":[],\"referenceAnswer\":\"参考答案原文\",\"improvementAdvice\":\"优先补充核心要点。\","
    "\"reviewKnowledgePoints\":[],\"fallbackUsed\":false}。"
)
_ASCII_TERM_PATTERN = re.compile(r"[A-Za-z][A-Za-z0-9+_.-]{1,}")
_ASCII_IGNORED_TERMS = {"query", "rewrite"}
_DOMAIN_TERMS = (
    "RAG",
    "Embedding",
    "Chunk",
    "Query Rewrite",
    "BM25",
    "Rerank",
    "Prompt",
    "Fine-tuning",
    "检索增强生成",
    "离线建库",
    "在线问答",
    "文档解析",
    "权限过滤",
    "查询改写",
    "问题理解",
    "向量检索",
    "混合检索",
    "关键词检索",
    "向量库",
    "向量化",
    "召回",
    "重排",
    "精排",
    "证据",
    "引用",
    "拒答",
    "上下文压缩",
    "评测闭环",
    "反馈闭环",
    "可追溯",
    "知识更新",
    "微调",
    "输出风格",
    "格式遵循",
)
_KEYWORD_ALIASES = {
    "rag": ("检索增强生成", "检索增强", "外部知识", "知识库问答"),
    "检索增强生成": ("RAG", "检索增强", "外部知识", "知识库问答"),
    "embedding": ("向量化", "向量表示", "嵌入模型", "转成向量"),
    "chunk": ("切分", "分块", "切片", "文本块", "知识片段"),
    "query rewrite": ("查询改写", "问题改写", "改写问题"),
    "bm25": ("关键词检索", "稀疏检索", "混合检索"),
    "rerank": ("重排", "精排", "重新排序"),
    "prompt": ("提示词", "上下文"),
    "fine-tuning": ("微调", "模型微调"),
    "向量化": ("Embedding", "嵌入", "向量表示"),
    "重排": ("Rerank", "精排", "排序"),
    "查询改写": ("Query Rewrite", "问题改写", "改写问题"),
    "离线建库": ("文档解析", "清洗", "切分", "向量库", "入库"),
    "在线问答": ("用户提问", "检索召回", "召回", "生成答案"),
}

logger = logging.getLogger("ai_service.practice.llm")
logger.setLevel(logging.INFO)
if not logger.handlers:
    stream_handler = logging.StreamHandler()
    stream_handler.setFormatter(logging.Formatter("%(asctime)s %(levelname)s [%(name)s] %(message)s"))
    logger.addHandler(stream_handler)
logger.propagate = False


class PracticeAgentService:
    """AI 智能刷题 Agent 服务。"""

    def grade_answer(self, request: PracticeGradeRequest) -> PracticeGradeResponse:
        """优先调用真实大模型评分，失败或未配置时使用本地规则兜底。"""
        logger.info(
            "【AI智能刷题流程-评分】收到答案评分请求：userId=%s questionCode=%s llmEnabled=%s",
            request.userId,
            request.questionCode,
            self._is_llm_enabled(),
        )

        # 优先走真实大模型，确保线上评分链路可通过 traceId 追踪。
        if self._is_llm_enabled():
            llm_response = self._grade_answer_by_llm(request)
            if llm_response is not None:
                return llm_response

        # 未启用大模型或大模型异常时，保留本地规则评分能力。
        logger.info(
            "【AI智能刷题流程-评分】未调用真实大模型，使用本地规则评分：model=%s baseUrlConfigured=%s",
            settings.ai_grading_model,
            bool(settings.ai_grading_base_url),
        )
        return self._grade_answer_by_local_rule(request)

    def discuss(self, request: PracticeDiscussRequest) -> PracticeDiscussResponse:
        """优先调用真实大模型生成讨论回复，失败或未配置时使用本地规则兜底。"""
        logger.info(
            "【AI智能刷题流程-讨论】收到非流式讨论请求：questionCode=%s historySize=%s llmEnabled=%s",
            request.questionCode,
            len(request.conversationHistory),
            self._is_llm_enabled(),
        )

        # 非流式接口用于普通追问，完整入参和完整返回会在模型调用处打印。
        if self._is_llm_enabled():
            llm_response = self._discuss_by_llm(request)
            if llm_response is not None:
                return llm_response

        # 大模型不可用时，不伪造讨论内容，避免误导用户。
        logger.info(
            "【AI智能刷题流程-讨论】未调用真实大模型，返回讨论不可用提示：model=%s baseUrlConfigured=%s",
            settings.ai_grading_model,
            bool(settings.ai_grading_base_url),
        )
        return self._discuss_by_local_rule(request)

    def stream_discuss(self, request: PracticeDiscussRequest) -> Iterator[str]:
        """流式生成本题讨论回复。"""
        logger.info(
            "【AI智能刷题流程-流式讨论】收到流式讨论请求：questionCode=%s historySize=%s llmEnabled=%s",
            request.questionCode,
            len(request.conversationHistory),
            self._is_llm_enabled(),
        )

        # 流式接口只在最终完成时打印汇总结果，避免 token 级日志刷屏。
        if self._is_llm_enabled():
            yield from self._stream_discuss_by_llm(request)
            return

        # 未启用真实模型时仍返回 SSE，保证 Java 后端和前端链路稳定。
        logger.info(
            "【AI智能刷题流程-流式讨论】未调用真实大模型，流式返回讨论不可用提示：model=%s baseUrlConfigured=%s",
            settings.ai_grading_model,
            bool(settings.ai_grading_base_url),
        )
        yield self._build_sse_event("message", {"content": _FALLBACK_DISCUSS_REPLY})
        yield self._build_sse_event("done", {})

    def _grade_answer_by_llm(self, request: PracticeGradeRequest) -> PracticeGradeResponse | None:
        """使用 LangChain 结构化输出完成答案评分。"""
        trace_id = self._new_trace_id()
        messages = self._build_grade_messages(request)
        start_time = time.perf_counter()
        logger.info(
            "【AI智能刷题流程-评分】准备调用大模型结构化评分：traceId=%s model=%s",
            trace_id,
            settings.ai_grading_model,
        )
        self._log_llm_request(trace_id, "答案评分", _GRADE_SYSTEM_PROMPT, messages, stream=False)
        try:
            grading = self._grade_model().invoke([SystemMessage(content=_GRADE_SYSTEM_PROMPT), *messages])
            grading = grading if isinstance(grading, PracticeGradeResponse) else PracticeGradeResponse.model_validate(grading)
            grading.fallbackUsed = False

            # LangChain 结构化输出成功后记录完整评分结果，便于按 traceId 复盘模型返回。
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            self._log_llm_response(trace_id, "答案评分", {"grading": grading}, elapsed_ms)
            logger.info(
                "【AI智能刷题流程-评分】大模型结构化评分完成：traceId=%s model=%s durationMs=%s score=%s",
                trace_id,
                settings.ai_grading_model,
                elapsed_ms,
                grading.score,
            )
            return grading
        except Exception as exc:  # noqa: BLE001 - 模型、网络和结构化解析异常统一进入兜底。
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            logger.warning(
                "【AI智能刷题流程-评分】大模型结构化评分失败，使用本地规则兜底：traceId=%s durationMs=%s error=%s",
                trace_id,
                elapsed_ms,
                exc,
                exc_info=True,
            )
            return None

    def _discuss_by_llm(self, request: PracticeDiscussRequest) -> PracticeDiscussResponse | None:
        """使用 LangChain Agent 生成本题讨论回复。"""
        trace_id = self._new_trace_id()
        start_time = time.perf_counter()
        messages = self._build_discuss_messages(request)
        logger.info(
            "【AI智能刷题流程-讨论】准备调用大模型非流式讨论：traceId=%s model=%s",
            trace_id,
            settings.ai_grading_model,
        )
        self._log_llm_request(trace_id, "本题讨论-非流式", _DISCUSSION_SYSTEM_PROMPT, messages, stream=False)
        try:
            result = self._discussion_agent().invoke({"messages": messages})
            reply = self._last_ai_reply(result).strip()
            if not reply:
                return None

            # 非流式讨论记录完整模型返回，排查时可直接看到最终回复内容。
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            self._log_llm_response(trace_id, "本题讨论-非流式", {"reply": reply, "rawResult": result}, elapsed_ms)
            logger.info(
                "【AI智能刷题流程-讨论】大模型非流式讨论完成：traceId=%s durationMs=%s replyChars=%s",
                trace_id,
                elapsed_ms,
                len(reply),
            )
            return PracticeDiscussResponse(reply=reply)
        except Exception as exc:  # noqa: BLE001 - 模型和图执行异常统一进入兜底。
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            logger.warning(
                "【AI智能刷题流程-讨论】大模型非流式讨论失败，使用本地兜底：traceId=%s durationMs=%s error=%s",
                trace_id,
                elapsed_ms,
                exc,
                exc_info=True,
            )
            return None

    def _stream_discuss_by_llm(self, request: PracticeDiscussRequest) -> Iterator[str]:
        """使用 LangChain 流式接口生成讨论回复。"""
        trace_id = self._new_trace_id()
        start_time = time.perf_counter()
        messages = self._build_discuss_messages(request)
        logger.info(
            "【AI智能刷题流程-流式讨论】准备调用大模型流式讨论：traceId=%s model=%s",
            trace_id,
            settings.ai_grading_model,
        )
        try:
            emitted_any = False
            full_reply_parts: list[str] = []

            # 当前 create_agent 在部分 OpenAI 兼容供应商下只产出图事件，不产出可见 token。
            # 流式接口优先走底层聊天模型，避免先等待 Agent 空流导致前端长时间“思考中”。
            for content in self._stream_discuss_with_model(messages, trace_id, start_time):
                emitted_any = True
                full_reply_parts.append(content)
                yield self._build_sse_event("message", {"content": content})

            # 模型原生 stream 不可用时，再尝试 Agent stream，保留 LangChain Agent 兜底能力。
            if not emitted_any:
                logger.warning("LangChain 模型原生流式未产出可见片段，切换 Agent 流式输出：traceId=%s", trace_id)
                for content in self._stream_discuss_with_agent(messages, trace_id, start_time):
                    emitted_any = True
                    full_reply_parts.append(content)
                    yield self._build_sse_event("message", {"content": content})

            # 双流式链路均无输出时，最后才回退非流式，避免前端长时间空白。
            if not emitted_any:
                logger.warning("LangChain 流式链路均无可见片段，切换非流式兜底：traceId=%s", trace_id)
                fallback_response = self._discuss_by_llm(request) or self._discuss_by_local_rule(request)
                full_reply_parts.append(fallback_response.reply)
                yield self._build_sse_event("message", {"content": fallback_response.reply})
            yield self._build_sse_event("done", {})
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            full_reply = "".join(full_reply_parts)
            self._log_llm_response(trace_id, "本题讨论-流式汇总", {"reply": full_reply}, elapsed_ms)
            logger.info(
                "【AI智能刷题流程-流式讨论】大模型流式讨论完成：traceId=%s durationMs=%s replyChars=%s",
                trace_id,
                elapsed_ms,
                len(full_reply),
            )
        except Exception as exc:  # noqa: BLE001 - 流式模型异常统一进入兜底。
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            logger.warning(
                "【AI智能刷题流程-流式讨论】大模型流式讨论失败：traceId=%s durationMs=%s error=%s",
                trace_id,
                elapsed_ms,
                exc,
                exc_info=True,
            )
            fallback_response = self._discuss_by_local_rule(request)
            yield self._build_sse_event("message", {"content": fallback_response.reply})
            yield self._build_sse_event("done", {})

    def _stream_discuss_with_agent(self, messages: list[BaseMessage], trace_id: str, start_time: float) -> Iterator[str]:
        """使用 LangChain Agent 流式输出讨论回复。"""
        event_count = 0
        content_count = 0
        self._log_llm_request(trace_id, "本题讨论-Agent流式", _DISCUSSION_SYSTEM_PROMPT, messages, stream=True)
        for chunk in self._discussion_agent().stream({"messages": messages}, stream_mode="messages", version="v2"):
            event_count += 1
            content = self._agent_stream_content(chunk)
            if content:
                content_count += 1
                self._log_visible_stream_chunk(trace_id, start_time, "agent", content_count, content)
                yield content

        # 记录事件数和可见文本数，用于定位供应商是否支持 Agent token 流。
        logger.info("【AI智能刷题流程-流式讨论】Agent流式事件统计：traceId=%s events=%s visibleChunks=%s", trace_id, event_count, content_count)

    def _stream_discuss_with_model(self, messages: list[BaseMessage], trace_id: str, start_time: float) -> Iterator[str]:
        """使用底层聊天模型原生流式输出讨论回复。"""
        chunk_count = 0
        model_messages = [SystemMessage(content=_DISCUSSION_SYSTEM_PROMPT), *messages]
        self._log_llm_request(trace_id, "本题讨论-模型原生流式", None, model_messages, stream=True)

        # 模型原生 stream 作为 Agent stream 的流式兜底，但仍保留同样的系统提示。
        for chunk in self._chat_model().stream(model_messages):
            content = self._chunk_content(chunk)
            if content:
                chunk_count += 1
                self._log_visible_stream_chunk(trace_id, start_time, "model", chunk_count, content)
                yield content

        # 记录模型原生流式片段数，便于排查模型供应商流式能力。
        logger.info("【AI智能刷题流程-流式讨论】模型原生流式片段统计：traceId=%s visibleChunks=%s", trace_id, chunk_count)

    def _log_visible_stream_chunk(self, trace_id: str, start_time: float, source: str, count: int, content: str) -> None:
        """记录可见流式片段输出情况。"""
        if count != 1 and count % 50 != 0:
            return

        # 日志只打印长度和耗时，避免用户答案或模型全文进入日志。
        elapsed_ms = round((time.perf_counter() - start_time) * 1000)
        logger.info(
            "【AI智能刷题流程-流式讨论】可见流式片段进度：traceId=%s source=%s count=%s chars=%s elapsedMs=%s",
            trace_id,
            source,
            count,
            len(content),
            elapsed_ms,
        )

    def _log_llm_request(self, trace_id: str, scene: str, system_prompt: str | None, messages: list[BaseMessage], stream: bool) -> None:
        """完整记录大模型调用入参。"""
        payload = {
            "scene": scene,
            "stream": stream,
            "model": settings.ai_grading_model,
            "modelProvider": settings.ai_grading_model_provider,
            "baseUrl": self._normalized_base_url() if settings.ai_grading_base_url.strip() else "",
            "timeoutSeconds": settings.ai_grading_timeout_seconds,
            "systemPrompt": system_prompt,
            "messages": [self._message_to_log_payload(message) for message in messages],
        }

        # 仅排除 API Key 等密钥配置，业务入参和提示词完整输出不截断。
        logger.info("【AI智能刷题-大模型调用入参】traceId=%s payload=%s", trace_id, self._to_pretty_json(payload))

    def _log_llm_response(self, trace_id: str, scene: str, response: Any, elapsed_ms: int) -> None:
        """完整记录大模型调用返回。"""
        payload = {
            "scene": scene,
            "durationMs": elapsed_ms,
            "response": self._to_log_payload(response),
        }

        # 非流式返回记录完整结果；流式返回只在调用结束后记录汇总文本。
        logger.info("【AI智能刷题-大模型调用返回】traceId=%s payload=%s", trace_id, self._to_pretty_json(payload))

    def _message_to_log_payload(self, message: BaseMessage) -> dict[str, Any]:
        """把 LangChain 消息转换为可读日志结构。"""
        return {
            "type": getattr(message, "type", message.__class__.__name__),
            "content": self._normalize_content(getattr(message, "content", "")),
            "additionalKwargs": self._to_log_payload(getattr(message, "additional_kwargs", {})),
        }

    def _to_pretty_json(self, value: Any) -> str:
        """使用 UTF-8 友好的 JSON 字符串输出日志。"""
        return json.dumps(self._to_log_payload(value), ensure_ascii=False, indent=2, default=str)

    def _to_log_payload(self, value: Any) -> Any:
        """递归转换复杂对象，确保日志不会因为不可序列化对象失败。"""
        if isinstance(value, BaseMessage):
            return self._message_to_log_payload(value)
        if isinstance(value, BaseModel):
            return value.model_dump(mode="json")
        if isinstance(value, dict):
            return {str(key): self._to_log_payload(item) for key, item in value.items()}
        if isinstance(value, (list, tuple, set)):
            return [self._to_log_payload(item) for item in value]
        return value

    def _build_grade_messages(self, request: PracticeGradeRequest) -> list[BaseMessage]:
        """构造评分消息列表。"""
        return [
            HumanMessage(content=self._build_grade_prompt(request)),
        ]

    def _build_discuss_messages(self, request: PracticeDiscussRequest) -> list[BaseMessage]:
        """构造本题讨论消息列表。"""
        messages: list[BaseMessage] = [
            HumanMessage(content=self._build_discuss_context_prompt(request)),
        ]

        # 历史消息由 Java 后端维护，只保留当前题短期上下文。
        for item in request.conversationHistory:
            content = item.content.strip()
            if not content:
                continue
            if item.role == "assistant":
                messages.append(AIMessage(content=content))
            else:
                messages.append(HumanMessage(content=content))
        messages.append(HumanMessage(content=f"用户当前疑问：{request.message}"))
        return messages

    def _build_grade_prompt(self, request: PracticeGradeRequest) -> str:
        """构造答案评分提示词。"""
        return (
            "请根据题目、参考答案和用户答案进行评分。\n"
            "score 必须是 0 到 100 的整数，correct 表示是否及格。\n"
            "hitPoints、missingPoints、problems、reviewKnowledgePoints 必须是简短数组。\n"
            "improvementAdvice 使用一到两句话概括最优先的改进方向。\n"
            "不要把题目分类当作必须命中的扣分项；允许用户使用同义表达。\n"
            f"{_GRADE_JSON_INSTRUCTION}\n"
            f"题目分类：{request.questionType}\n"
            f"题目：{request.question}\n"
            f"参考答案：{request.standardAnswer}\n"
            f"用户答案：{request.userAnswer}"
        )

    def _build_discuss_context_prompt(self, request: PracticeDiscussRequest) -> str:
        """构造本题讨论上下文提示词。"""
        grading_summary = request.gradingSummary or "暂无评分摘要"
        last_answer = request.lastUserAnswer or "暂无"
        return (
            "请基于下方当前题上下文回答后续用户疑问。\n"
            "要求：不要复述题目原文；优先解释评分依据、知识点、用户答案缺口和可改进表达。\n"
            f"题目分类：{request.questionType}\n"
            f"题目：{request.question}\n"
            f"参考答案：{request.standardAnswer}\n"
            f"用户最近一次答案：{last_answer}\n"
            f"AI评分结果摘要：{grading_summary}\n"
            "下面会继续给出当前题历史追问消息和本轮最新疑问。"
        )

    def _grade_answer_by_local_rule(self, request: PracticeGradeRequest) -> PracticeGradeResponse:
        """结合参考答案关键词和本地规则给答案评分。"""
        keywords = self._build_keywords(request)
        answer = request.userAnswer.strip()

        # 命中判断保持大小写和空白不敏感，兼容英文技术名词及中文同义表达。
        normalized_answer = self._normalize_text(answer)
        hit_points: list[str] = []
        missing_points: list[str] = []
        for keyword in keywords:
            if self._keyword_hit(keyword, normalized_answer):
                hit_points.append(f"已覆盖「{keyword}」相关核心要点")
            else:
                missing_points.append(f"待补充「{keyword}」相关说明")

        # 分数由关键词覆盖度和内容完整度共同决定。
        score = self._calculate_score(len(hit_points), len(keywords), len(answer))
        problems = self._build_problems(answer, not hit_points)
        advice = self._build_advice(score, missing_points, problems)
        return PracticeGradeResponse(
            score=score,
            correct=score >= 60,
            hitPoints=hit_points,
            missingPoints=missing_points,
            problems=problems,
            referenceAnswer=request.standardAnswer,
            improvementAdvice=advice,
            reviewKnowledgePoints=self._review_points(request.questionType, missing_points),
            fallbackUsed=True,
        )

    def _discuss_by_local_rule(self, request: PracticeDiscussRequest) -> PracticeDiscussResponse:
        """大模型异常时返回无法继续探讨的兜底提示。"""
        _ = request

        # 讨论能力不再使用本地规则伪造，避免给用户造成模型仍可追问的误解。
        return PracticeDiscussResponse(reply=_FALLBACK_DISCUSS_REPLY)

    def _build_keywords(self, request: PracticeGradeRequest) -> list[str]:
        """只从参考答案构建必答关键词。"""
        keywords: list[str] = []

        # 题目分类不作为扣分项，避免用户答对但没复述分类名时被误扣。
        self._add_ascii_terms(keywords, request.standardAnswer)
        self._add_domain_terms(keywords, request.standardAnswer)
        for token in _SPLIT_PATTERN.split(request.standardAnswer):
            self._add_keyword(keywords, token)
        return self._filter_excluded_keywords(keywords, request)[:_MAX_KEYWORDS]

    def _add_ascii_terms(self, keywords: list[str], source: str) -> None:
        """提取英文技术词。"""
        for match in _ASCII_TERM_PATTERN.finditer(source):
            term = match.group()
            if term.lower() not in _ASCII_IGNORED_TERMS:
                self._add_keyword(keywords, term)

    def _add_domain_terms(self, keywords: list[str], source: str) -> None:
        """从参考答案中提取常见领域短语。"""
        normalized_source = self._normalize_text(source)
        for term in _DOMAIN_TERMS:
            if self._normalize_text(term) in normalized_source:
                self._add_keyword(keywords, term)

    def _add_keyword(self, keywords: list[str], value: str | None) -> None:
        """追加单个关键词。"""
        if not value:
            return
        keyword = value.strip()
        if 2 <= len(keyword) <= 24 and keyword not in keywords:
            keywords.append(keyword)

    def _filter_excluded_keywords(self, keywords: list[str], request: PracticeGradeRequest) -> list[str]:
        """过滤题目和分类中已经包含的宽泛词。"""
        excluded_sources = [request.question, request.questionType]
        return [keyword for keyword in keywords if not self._is_excluded_keyword(keyword, excluded_sources)]

    def _is_excluded_keyword(self, keyword: str, excluded_sources: list[str]) -> bool:
        """判断关键词是否属于非扣分来源。"""
        normalized_keyword = self._normalize_text(keyword)
        for source in excluded_sources:
            normalized_source = self._normalize_text(source)
            if normalized_source and (normalized_keyword in normalized_source or normalized_source in normalized_keyword):
                return True
        return False

    def _keyword_hit(self, keyword: str, normalized_answer: str) -> bool:
        """判断答案是否覆盖关键词或同义表达。"""
        if self._normalize_text(keyword) in normalized_answer:
            return True

        # 同义表达只用于放宽命中，不额外增加必答项。
        aliases = _KEYWORD_ALIASES.get(keyword.lower(), ())
        return any(self._normalize_text(alias) in normalized_answer for alias in aliases)

    def _normalize_text(self, value: str | None) -> str:
        """规整文本用于大小写和空白不敏感匹配。"""
        if not value:
            return ""
        return re.sub(r"\s+", "", value).lower()

    def _calculate_score(self, hit_count: int, total_count: int, answer_length: int) -> int:
        """计算百分制得分。"""
        if total_count <= 0:
            return min(100, max(0, answer_length))
        keyword_score = round(hit_count / total_count * 80)
        content_score = 20 if answer_length >= 20 else answer_length
        return min(100, max(0, keyword_score + content_score))

    def _build_problems(self, answer: str, no_keyword_hit: bool) -> list[str]:
        """生成问题点。"""
        problems: list[str] = []
        if len(answer) < 20:
            problems.append("回答较简略，建议补充关键流程和原因说明")
        if no_keyword_hit:
            problems.append("未明显覆盖参考答案中的核心关键词")
        return problems

    def _build_advice(self, score: int, missing_points: list[str], problems: list[str]) -> str:
        """生成优化建议。"""
        if score >= 90 and not missing_points:
            return "回答已经很完整，继续保持这种结构化表达。"
        if missing_points:
            advice_points = [self._missing_point_to_advice(item) for item in missing_points]
            return "建议优先补充：" + "；".join(advice_points) + "。"
        if problems:
            return "；".join(problems) + "。"
        return "整体回答不错，可以再补充一个工程实践案例。"

    def _missing_point_to_advice(self, missing_point: str) -> str:
        """把缺失点转换为简短建议。"""
        point = missing_point.replace("待补充「", "").replace("」相关说明", "")
        return f"补充「{point}」"

    def _review_points(self, question_type: str, missing_points: list[str]) -> list[str]:
        """提取建议复习点。"""
        points = [item.replace("待补充「", "").replace("」相关说明", "") for item in missing_points]
        return points or [question_type]

    def _is_llm_enabled(self) -> bool:
        """判断是否具备真实大模型调用配置。"""
        model = settings.ai_grading_model.strip()
        api_key = settings.ai_grading_api_key.strip()

        # LOCAL_RULE 或占位符配置表示仅使用本地规则，不发起外部模型请求。
        return (
            bool(settings.ai_grading_base_url.strip())
            and bool(api_key)
            and model.upper() != _LOCAL_RULE_MODEL
            and api_key != _API_KEY_PLACEHOLDER
        )

    def _grade_model(self):
        """创建答案评分结构化聊天模型。"""
        # DeepSeek reasoning 模型不支持 Agent 结构化输出触发的 tool_choice。
        # JSON mode 只使用 response_format，不绑定工具，兼容 OpenAI 与 DeepSeek。
        return self._chat_model().with_structured_output(PracticeGradeResponse, method="json_mode")

    def _discussion_agent(self):
        """创建本题讨论 Agent。"""
        return create_agent(
            model=self._chat_model(),
            tools=[],
            system_prompt=_DISCUSSION_SYSTEM_PROMPT,
        )

    def _chat_model(self):
        """使用 LangChain 推荐入口初始化聊天模型。"""
        kwargs: dict[str, Any] = {
            "temperature": 0.2,
            "timeout": settings.ai_grading_timeout_seconds,
        }
        if settings.ai_grading_api_key.strip():
            kwargs["api_key"] = settings.ai_grading_api_key
        if settings.ai_grading_base_url.strip():
            kwargs["base_url"] = self._normalized_base_url()
        if settings.ai_grading_model_provider.strip():
            kwargs["model_provider"] = settings.ai_grading_model_provider.strip()
        if self._is_deepseek_provider():
            kwargs["extra_body"] = _DEEPSEEK_THINKING_DISABLED_BODY

        # init_chat_model 支持通过 provider/model 抽象切换底层模型供应商。
        return init_chat_model(settings.ai_grading_model, **kwargs)

    def _is_deepseek_provider(self) -> bool:
        """判断当前模型配置是否指向 DeepSeek 服务。"""
        provider = settings.ai_grading_model_provider.strip().lower()
        model = settings.ai_grading_model.strip().lower()
        base_url = settings.ai_grading_base_url.strip().lower()

        # DeepSeek V4 默认开启思考模式，这里按供应商、模型名或官方域名识别后统一关闭。
        return provider == "deepseek" or model.startswith("deepseek-") or "deepseek.com" in base_url

    def _normalized_base_url(self) -> str:
        """规整 OpenAI 兼容基础地址。"""
        base_url = settings.ai_grading_base_url.strip().rstrip("/")
        if base_url.endswith(_CHAT_COMPLETIONS_PATH):
            return base_url[: -len(_CHAT_COMPLETIONS_PATH)]
        return base_url

    def _message_content(self, message: Any) -> str:
        """读取 LangChain 消息内容。"""
        content = getattr(message, "content", "")
        return self._normalize_content(content).strip()

    def _last_ai_reply(self, result: dict[str, Any]) -> str:
        """从 Agent 执行结果中读取最后一条 AI 回复。"""
        messages = result.get("messages")
        if not isinstance(messages, list):
            return ""

        # create_agent 返回的状态中，最新模型回复通常位于 messages 最后一条。
        for message in reversed(messages):
            if isinstance(message, AIMessage) or getattr(message, "type", "") == "ai":
                return self._message_content(message)
        return ""

    def _agent_stream_content(self, chunk: Any) -> str:
        """读取 Agent 流式事件中的文本片段。"""
        if isinstance(chunk, tuple) and chunk:
            return self._chunk_content(chunk[0])
        if isinstance(chunk, dict):
            return self._last_ai_reply(chunk)
        return self._chunk_content(chunk)

    def _chunk_content(self, chunk: Any) -> str:
        """读取 LangChain 流式消息片段内容。"""
        content = getattr(chunk, "content", "")
        normalized_content = self._normalize_content(content)
        if normalized_content:
            return normalized_content

        # 某些模型集成会把 token 放在 text 或 content_blocks 字段中。
        text = getattr(chunk, "text", "")
        if isinstance(text, str):
            return text
        return self._normalize_content(getattr(chunk, "content_blocks", ""))

    def _normalize_content(self, content: Any) -> str:
        """规整 LangChain 消息内容。"""
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            return "".join(self._normalize_content(item) for item in content)
        if isinstance(content, dict):
            return str(content.get("text") or content.get("content") or content.get("input") or "")
        return "" if content is None else str(content)

    def _build_sse_event(self, event: str, data: dict[str, Any]) -> str:
        """构造内部 SSE 事件文本。"""
        payload = json.dumps(data, ensure_ascii=False)
        return f"event: {event}\ndata: {payload}\n\n"

    def _new_trace_id(self) -> str:
        """生成单次大模型调用追踪 ID。"""
        return uuid.uuid4().hex[:12]


practice_agent_service = PracticeAgentService()
