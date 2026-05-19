from __future__ import annotations

import json
import logging
import time
import uuid
from collections.abc import Iterator
from typing import Any

from langchain.agents import create_agent
from langchain.chat_models import init_chat_model
from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, SystemMessage
from pydantic import BaseModel

from app.config.constants import AI_GRADING_API_KEY_PLACEHOLDER, CHAT_COMPLETIONS_PATH, LOCAL_RULE_MODEL
from app.config.settings import settings
from app.schemas.practice import (
    PracticeDiscussRequest,
    PracticeDiscussResponse,
    PracticeGradeRequest,
    PracticeGradeResponse,
)

_FALLBACK_DISCUSS_REPLY = "抱歉，当前大模型调用异常，仅保留兜底策略评分功能，无法和您进行探讨。"
_GRADE_SYSTEM_PROMPT = (
    "你是一名资深AI Agent开发工程师，熟悉领域内各大技术栈，我将提交一段题目、参考答案、用户答案给你。\n"
    "请你进行评分，并且使用我要求的Json结构化输出。\n"
    "允许用户使用同义表达，结合你自身和知识和参考答案一起点评，不能强求用户答案和参考答案一模一样，意义相同即得分。\n"
)
_DISCUSSION_SYSTEM_PROMPT = "你是一名资深AI Agent开发工程师，熟悉领域内各大技术栈，请围绕当前刷题上下文为用户解答疑惑。"
_DEEPSEEK_THINKING_DISABLED_BODY = {"thinking": {"type": "disabled"}}
logger = logging.getLogger("ai_service.practice.llm")
logger.setLevel(logging.INFO)
if not logger.handlers:
    stream_handler = logging.StreamHandler()
    stream_handler.setFormatter(logging.Formatter("%(asctime)s %(levelname)s [%(name)s] %(message)s"))
    logger.addHandler(stream_handler)
logger.propagate = False


class PracticeAgentService:
    """AI 智能刷题 Agent 服务。"""

    def grade_answer(self, request: PracticeGradeRequest) -> PracticeGradeResponse | None:
        """调用真实大模型评分，失败时交由 Java 后端本地兜底。"""
        logger.info(
            "【AI智能刷题流程-评分】收到答案评分请求：userId=%s questionCode=%s llmEnabled=%s",
            request.userId,
            request.questionCode,
            self._is_llm_enabled(),
        )

        if self._is_llm_enabled():
            return self._grade_answer_by_llm(request)

        logger.info(
            "【AI智能刷题流程-评分】未启用真实大模型，返回失败并交由 Java 后端本地兜底：model=%s baseUrlConfigured=%s",
            settings.ai_grading_model,
            bool(settings.ai_grading_base_url),
        )
        return None

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
        except Exception as exc:  # noqa: BLE001 - 模型、网络和结构化解析异常统一交由 Java 后端兜底。
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            logger.warning(
                "【AI智能刷题流程-评分】大模型结构化评分失败，交由 Java 后端本地兜底：traceId=%s durationMs=%s error=%s",
                trace_id,
                elapsed_ms,
                exc,
                exc_info=True,
            )
            return None

    def _generate_discuss_reply_by_llm(self, request: PracticeDiscussRequest) -> PracticeDiscussResponse | None:
        """使用 LangChain Agent 为流式链路兜底生成完整讨论回复。"""
        trace_id = self._new_trace_id()
        start_time = time.perf_counter()
        messages = self._build_discuss_messages(request)
        logger.info(
            "【AI智能刷题流程-讨论】准备调用大模型完整讨论兜底：traceId=%s model=%s",
            trace_id,
            settings.ai_grading_model,
        )
        self._log_llm_request(trace_id, "本题讨论-流式兜底", _DISCUSSION_SYSTEM_PROMPT, messages, stream=False)
        try:
            result = self._discussion_agent().invoke({"messages": messages})
            reply = self._last_ai_reply(result).strip()
            if not reply:
                return None

            # 流式无可见片段时记录完整模型返回，排查时可直接看到最终回复内容。
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            self._log_llm_response(trace_id, "本题讨论-流式兜底", {"reply": reply, "rawResult": result}, elapsed_ms)
            logger.info(
                "【AI智能刷题流程-讨论】大模型完整讨论兜底完成：traceId=%s durationMs=%s replyChars=%s",
                trace_id,
                elapsed_ms,
                len(reply),
            )
            return PracticeDiscussResponse(reply=reply)
        except Exception as exc:  # noqa: BLE001 - 模型和图执行异常统一进入兜底。
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            logger.warning(
                "【AI智能刷题流程-讨论】大模型完整讨论兜底失败，使用本地兜底：traceId=%s durationMs=%s error=%s",
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
                fallback_response = self._generate_discuss_reply_by_llm(request) or self._discuss_by_local_rule(request)
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
        """构造答案评分用户输入。"""
        return (
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
            f"题目分类：{request.questionType}\n"
            f"题目：{request.question}\n"
            f"参考答案：{request.standardAnswer}\n"
            f"用户最近一次答案：{last_answer}\n"
            f"AI评分结果摘要：{grading_summary}\n"
            "下面会继续给出当前题历史追问消息和本轮最新疑问。"
        )

    def _discuss_by_local_rule(self, request: PracticeDiscussRequest) -> PracticeDiscussResponse:
        """大模型异常时返回无法继续探讨的兜底提示。"""
        _ = request

        # 讨论能力不再使用本地规则伪造，避免给用户造成模型仍可追问的误解。
        return PracticeDiscussResponse(reply=_FALLBACK_DISCUSS_REPLY)

    def _is_llm_enabled(self) -> bool:
        """判断是否具备真实大模型调用配置。"""
        model = settings.ai_grading_model.strip()
        api_key = settings.ai_grading_api_key.strip()

        # LOCAL_RULE 或占位符配置表示 ai-service 不发起外部模型请求。
        return (
            bool(settings.ai_grading_base_url.strip())
            and bool(api_key)
            and model.upper() != LOCAL_RULE_MODEL
            and api_key != AI_GRADING_API_KEY_PLACEHOLDER
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
        if base_url.endswith(CHAT_COMPLETIONS_PATH):
            return base_url[: -len(CHAT_COMPLETIONS_PATH)]
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
