from __future__ import annotations

import json
import logging
import re
import time
import urllib.error
import urllib.request
import uuid
from collections.abc import Iterator
from typing import Any

from app.config.settings import settings
from app.schemas.practice import (
    PracticeDiscussRequest,
    PracticeDiscussResponse,
    PracticeGradeRequest,
    PracticeGradeResponse,
    PracticeRelevanceRequest,
    PracticeRelevanceResponse,
)

_SPLIT_PATTERN = re.compile(r"[\s，。、；：,.!?！？（）()\"'“”‘’]+")
_MAX_KEYWORDS = 10
_LOCAL_RULE_MODEL = "LOCAL_RULE"
_API_KEY_PLACEHOLDER = "AI_GRADING_API_KEY占位符"
_CHAT_COMPLETIONS_PATH = "/chat/completions"
_LLM_RESPONSE_PREVIEW_LENGTH = 200
_FALLBACK_DISCUSS_REPLY = "抱歉，当前大模型调用异常，仅保留兜底策略评分功能，无法和您进行探讨。"
_ASCII_TERM_PATTERN = re.compile(r"[A-Za-z][A-Za-z0-9+_.-]{1,}")
_ASCII_IGNORED_TERMS = {"query", "rewrite"}
_UNRELATED_WORDS = {"天气", "新闻", "股票", "旅游", "做饭", "写诗", "翻译", "笑话", "帅", "好看", "星座"}
_PRACTICE_RELATED_WORDS = {"题", "答案", "回答", "评分", "分数", "知识点", "技术", "概念", "原理", "为什么", "怎么", "如何"}
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
        if self._is_llm_enabled():
            llm_response = self._grade_answer_by_llm(request)
            if llm_response is not None:
                return llm_response

        # 未启用大模型或大模型异常时，保留本地规则评分能力。
        logger.info(
            "未调用真实大模型，使用本地规则评分：model=%s baseUrlConfigured=%s",
            settings.ai_grading_model,
            bool(settings.ai_grading_base_url),
        )
        return self._grade_answer_by_local_rule(request)

    def discuss(self, request: PracticeDiscussRequest) -> PracticeDiscussResponse:
        """优先调用真实大模型生成讨论回复，失败或未配置时使用本地规则兜底。"""
        if self._is_llm_enabled():
            llm_response = self._discuss_by_llm(request)
            if llm_response is not None:
                return llm_response

        # 大模型不可用时，不再伪造讨论内容，只提示当前无法探讨。
        logger.info(
            "未调用真实大模型，返回讨论不可用提示：model=%s baseUrlConfigured=%s",
            settings.ai_grading_model,
            bool(settings.ai_grading_base_url),
        )
        return self._discuss_by_local_rule(request)

    def stream_discuss(self, request: PracticeDiscussRequest) -> Iterator[str]:
        """流式生成本题讨论回复。"""
        if self._is_llm_enabled():
            yield from self._stream_discuss_by_llm(request)
            return

        # 未启用真实模型时仍返回 SSE，保证 Java 后端和前端链路稳定。
        logger.info(
            "未调用真实大模型，流式返回讨论不可用提示：model=%s baseUrlConfigured=%s",
            settings.ai_grading_model,
            bool(settings.ai_grading_base_url),
        )
        yield self._build_sse_event("message", {"content": _FALLBACK_DISCUSS_REPLY})
        yield self._build_sse_event("done", {})

    def judge_relevance(self, request: PracticeRelevanceRequest) -> PracticeRelevanceResponse:
        """优先调用真实大模型判断输入是否与刷题上下文相关。"""
        if self._is_llm_enabled():
            llm_response = self._judge_relevance_by_llm(request)
            if llm_response is not None:
                return llm_response

        # 未配置真实大模型时，使用保守本地规则兜底，避免影响正常答题。
        logger.info(
            "未调用真实大模型，使用本地规则判断相关性：model=%s baseUrlConfigured=%s",
            settings.ai_grading_model,
            bool(settings.ai_grading_base_url),
        )
        return self._judge_relevance_by_local_rule(request)

    def _grade_answer_by_local_rule(self, request: PracticeGradeRequest) -> PracticeGradeResponse:
        """结合 RAG 片段和本地规则给答案评分。"""
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

    def _judge_relevance_by_local_rule(self, request: PracticeRelevanceRequest) -> PracticeRelevanceResponse:
        """使用本地规则兜底判断输入相关性。"""
        normalized_message = self._normalize_text(request.message)
        if any(word in request.message for word in _UNRELATED_WORDS):
            return PracticeRelevanceResponse(relevant=False, reason="命中明显无关闲聊词")

        # 命中题目、参考答案、分类或常见刷题词时视为相关。
        related_sources = [request.question, request.standardAnswer, request.questionType]
        if any(self._normalize_text(source) and self._normalize_text(source) in normalized_message for source in related_sources):
            return PracticeRelevanceResponse(relevant=True, reason="命中当前题上下文")
        if any(word in request.message for word in _PRACTICE_RELATED_WORDS):
            return PracticeRelevanceResponse(relevant=True, reason="命中刷题相关表达")
        return PracticeRelevanceResponse(relevant=True, reason="本地规则保守放行")

    def _grade_answer_by_llm(self, request: PracticeGradeRequest) -> PracticeGradeResponse | None:
        """调用 OpenAI 兼容大模型接口完成答案评分。"""
        trace_id = self._new_trace_id()
        prompt = self._build_grade_prompt(request)
        messages = self._build_messages(prompt)

        # 日志会打印到 uvicorn/Python 终端，便于确认真实请求是否发出。
        content = self._call_chat_completion(trace_id, "answer_grade", messages)
        if not content:
            return None

        # 大模型必须返回结构化 JSON，解析失败时走本地规则兜底。
        try:
            data = self._extract_json_object(content)
            score = self._normalize_score(data.get("score"))
            return PracticeGradeResponse(
                score=score,
                correct=self._normalize_bool(data.get("correct"), score >= 60),
                hitPoints=self._normalize_string_list(data.get("hitPoints")),
                missingPoints=self._normalize_string_list(data.get("missingPoints")),
                problems=self._normalize_string_list(data.get("problems")),
                referenceAnswer=str(data.get("referenceAnswer") or request.standardAnswer),
                improvementAdvice=str(data.get("improvementAdvice") or "建议对照参考答案补充关键要点。"),
                reviewKnowledgePoints=self._normalize_string_list(data.get("reviewKnowledgePoints")) or [request.questionType],
                fallbackUsed=False,
            )
        except (TypeError, ValueError, json.JSONDecodeError) as exc:
            logger.warning("大模型评分结果解析失败，使用本地规则兜底：traceId=%s error=%s", trace_id, exc)
            return None

    def _discuss_by_llm(self, request: PracticeDiscussRequest) -> PracticeDiscussResponse | None:
        """调用 OpenAI 兼容大模型接口生成学习讨论回复。"""
        trace_id = self._new_trace_id()
        prompt = self._build_discuss_prompt(request)
        messages = self._build_messages(prompt)

        # 讨论回复只需要自然语言文本，空回复时走本地规则兜底。
        content = self._call_chat_completion(trace_id, "practice_discuss", messages)
        if not content:
            return None
        return PracticeDiscussResponse(reply=content.strip())

    def _judge_relevance_by_llm(self, request: PracticeRelevanceRequest) -> PracticeRelevanceResponse | None:
        """调用 OpenAI 兼容大模型接口判断输入相关性。"""
        trace_id = self._new_trace_id()
        prompt = self._build_relevance_prompt(request)
        messages = self._build_messages(prompt)

        # 相关性判断只要求返回短 JSON，解析失败时走本地保守规则。
        content = self._call_chat_completion(trace_id, "practice_relevance", messages)
        if not content:
            return None
        try:
            data = self._extract_json_object(content)
            relevant = self._normalize_bool(data.get("relevant"), True)
            reason = str(data.get("reason") or "")[:120]
            return PracticeRelevanceResponse(relevant=relevant, reason=reason)
        except (TypeError, ValueError, json.JSONDecodeError) as exc:
            logger.warning("大模型相关性判断解析失败，使用本地规则兜底：traceId=%s error=%s", trace_id, exc)
            return None

    def _stream_discuss_by_llm(self, request: PracticeDiscussRequest) -> Iterator[str]:
        """调用 OpenAI 兼容大模型接口流式生成讨论回复。"""
        trace_id = self._new_trace_id()
        prompt = self._build_discuss_prompt(request)
        messages = self._build_messages(prompt)
        emitted_any = False
        for content in self._call_chat_completion_stream(trace_id, "practice_discuss_stream", messages):
            emitted_any = True
            yield self._build_sse_event("message", {"content": content})

        # 如果供应商没有返回任何可见 token，则回落为非流式调用，避免前端一直空白。
        if not emitted_any:
            fallback_response = self._discuss_by_llm(request) or self._discuss_by_local_rule(request)
            yield self._build_sse_event("message", {"content": fallback_response.reply})
        yield self._build_sse_event("done", {})

    def _call_chat_completion(self, trace_id: str, scene: str, messages: list[dict[str, str]]) -> str | None:
        """调用 OpenAI 兼容 chat completions 接口，并输出调用前后日志。"""
        url = self._chat_completion_url()
        start_time = time.perf_counter()
        payload = {
            "model": settings.ai_grading_model,
            "messages": messages,
            "temperature": 0.2,
        }

        # 调用前日志不打印密钥和完整提示词，避免终端泄露敏感信息。
        logger.info(
            "准备调用真实大模型：traceId=%s scene=%s model=%s url=%s promptChars=%s",
            trace_id,
            scene,
            settings.ai_grading_model,
            url,
            sum(len(message["content"]) for message in messages),
        )

        try:
            request = self._build_http_request(url, payload)
            with urllib.request.urlopen(request, timeout=settings.ai_grading_timeout_seconds) as response:
                raw_body = response.read().decode("utf-8")

            # 调用后日志包含耗时和 usage，便于在终端确认真实模型已响应。
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            response_data = json.loads(raw_body)
            content = self._read_chat_content(response_data)
            logger.info(
                "真实大模型调用完成：traceId=%s scene=%s model=%s durationMs=%s usage=%s responsePreview=%s",
                trace_id,
                scene,
                settings.ai_grading_model,
                elapsed_ms,
                response_data.get("usage"),
                content[:_LLM_RESPONSE_PREVIEW_LENGTH],
            )
            return content
        except (TimeoutError, OSError, urllib.error.URLError, json.JSONDecodeError, KeyError, UnicodeDecodeError) as exc:
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            logger.warning("真实大模型调用失败：traceId=%s scene=%s durationMs=%s error=%s", trace_id, scene, elapsed_ms, exc)
            return None

    def _call_chat_completion_stream(self, trace_id: str, scene: str, messages: list[dict[str, str]]) -> Iterator[str]:
        """调用 OpenAI 兼容 chat completions 流式接口。"""
        url = self._chat_completion_url()
        start_time = time.perf_counter()
        payload = {
            "model": settings.ai_grading_model,
            "messages": messages,
            "temperature": 0.2,
            "stream": True,
        }

        # 流式调用只记录概要信息，避免泄露完整提示词或用户答案。
        logger.info(
            "准备调用真实大模型流式接口：traceId=%s scene=%s model=%s url=%s promptChars=%s",
            trace_id,
            scene,
            settings.ai_grading_model,
            url,
            sum(len(message["content"]) for message in messages),
        )
        try:
            request = self._build_http_request(url, payload)
            with urllib.request.urlopen(request, timeout=settings.ai_grading_timeout_seconds) as response:
                for raw_line in response:
                    content = self._read_stream_content(raw_line)
                    if content:
                        yield content
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            logger.info("真实大模型流式调用完成：traceId=%s scene=%s durationMs=%s", trace_id, scene, elapsed_ms)
        except (TimeoutError, OSError, urllib.error.URLError, json.JSONDecodeError, KeyError, UnicodeDecodeError) as exc:
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            logger.warning("真实大模型流式调用失败：traceId=%s scene=%s durationMs=%s error=%s", trace_id, scene, elapsed_ms, exc)

    def _build_keywords(self, request: PracticeGradeRequest) -> list[str]:
        """只从参考答案构建必答关键词。"""
        keywords: list[str] = []

        # 题目分类和检索片段不作为扣分项，避免用户答对但没复述分类名时被误扣。
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
        return bool(settings.ai_grading_base_url.strip()) and bool(api_key) and model.upper() != _LOCAL_RULE_MODEL and api_key != _API_KEY_PLACEHOLDER

    def _chat_completion_url(self) -> str:
        """根据配置生成 OpenAI 兼容 chat completions 地址。"""
        base_url = settings.ai_grading_base_url.strip().rstrip("/")
        if base_url.endswith(_CHAT_COMPLETIONS_PATH):
            return base_url
        return f"{base_url}{_CHAT_COMPLETIONS_PATH}"

    def _build_http_request(self, url: str, payload: dict[str, Any]) -> urllib.request.Request:
        """构造 UTF-8 JSON 请求对象。"""
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        headers = {
            "Authorization": f"Bearer {settings.ai_grading_api_key}",
            "Content-Type": "application/json; charset=utf-8",
        }

        # 使用标准库避免新增依赖，保持本次改动足够小。
        return urllib.request.Request(url=url, data=body, headers=headers, method="POST")

    def _build_messages(self, prompt: str) -> list[dict[str, str]]:
        """构造大模型消息列表。"""
        return [
            {"role": "system", "content": "你是严谨的 AI 学习助手，请使用简体中文回答。"},
            {"role": "user", "content": prompt},
        ]

    def _build_grade_prompt(self, request: PracticeGradeRequest) -> str:
        """构造答案评分提示词。"""
        return (
            "请根据题目、参考答案和用户答案进行评分，只返回 JSON，不要返回 Markdown。\n"
            "JSON 字段为：score、correct、hitPoints、missingPoints、problems、referenceAnswer、"
            "improvementAdvice、reviewKnowledgePoints。\n"
            "score 必须是 0 到 100 的整数，correct 表示是否及格。\n"
            "hitPoints 和 missingPoints 必须返回简短的总结式数组，每项只表达一个要点。\n"
            "improvementAdvice 使用一到两句话概括最优先的改进方向，不要输出冗长段落。\n"
            "不要把题目分类当作必须命中的扣分项；允许用户使用同义表达，只按是否覆盖参考答案含义评分。\n"
            f"题目分类：{request.questionType}\n"
            f"题目：{request.question}\n"
            f"参考答案：{request.standardAnswer}\n"
            f"用户答案：{request.userAnswer}"
        )

    def _build_discuss_prompt(self, request: PracticeDiscussRequest) -> str:
        """构造学习讨论提示词。"""
        return (
            "请围绕当前刷题内容，用简洁、清晰的中文回答用户疑问。\n"
            "不要复述用户疑问或题目原文，直接给出解释、示例或改进建议。\n"
            "如果用户询问刚刚的题目、刚刚提交的答案或评分依据，请优先使用下方上下文准确回答。\n"
            f"题目分类：{request.questionType}\n"
            f"题目：{request.question}\n"
            f"参考答案：{request.standardAnswer}\n"
            f"用户刚刚提交的答案：{request.lastUserAnswer or '暂无'}\n"
            f"用户疑问：{request.message}"
        )

    def _build_relevance_prompt(self, request: PracticeRelevanceRequest) -> str:
        """构造输入相关性判断提示词。"""
        return (
            "请判断用户输入是否与刷题、回答当前面试题、讨论当前题技术点、泛技术学习问题有关。\n"
            "只返回 JSON，不要返回 Markdown。JSON 字段：relevant、reason。\n"
            "相关示例：提交答案、问刚刚答案是什么、追问知识点、要求解释技术概念、请求重答或下一题。\n"
            "无关示例：天气、新闻、股票、旅游、做饭、写诗、闲聊颜值、与技术学习无关的翻译或娱乐请求。\n"
            "不确定时请返回 relevant=true，避免误伤正常学习表达。\n"
            f"当前阶段：{request.phase}\n"
            f"题目分类：{request.questionType}\n"
            f"题目：{request.question}\n"
            f"参考答案：{request.standardAnswer}\n"
            f"用户输入：{request.message}"
        )

    def _read_chat_content(self, response_data: dict[str, Any]) -> str:
        """读取 OpenAI 兼容响应中的文本内容。"""
        choices = response_data.get("choices")
        if not isinstance(choices, list) or not choices:
            raise KeyError("choices")

        # 兼容 message.content 格式，保持解析逻辑明确。
        message = choices[0].get("message")
        if not isinstance(message, dict):
            raise KeyError("message")
        content = message.get("content")
        if not isinstance(content, str):
            raise KeyError("content")
        return content.strip()

    def _read_stream_content(self, raw_line: bytes) -> str:
        """读取 OpenAI 兼容流式响应中的单个文本片段。"""
        line = raw_line.decode("utf-8").strip()
        if not line.startswith("data:"):
            return ""
        data_text = line.removeprefix("data:").strip()
        if not data_text or data_text == "[DONE]":
            return ""

        # Chat Completions 流式响应通常位于 choices[0].delta.content。
        data = json.loads(data_text)
        choices = data.get("choices")
        if not isinstance(choices, list) or not choices:
            return ""
        delta = choices[0].get("delta")
        if not isinstance(delta, dict):
            return ""
        content = delta.get("content")
        return content if isinstance(content, str) else ""

    def _build_sse_event(self, event: str, data: dict[str, Any]) -> str:
        """构造内部 SSE 事件文本。"""
        payload = json.dumps(data, ensure_ascii=False)
        return f"event: {event}\ndata: {payload}\n\n"

    def _extract_json_object(self, content: str) -> dict[str, Any]:
        """从大模型文本中提取 JSON 对象。"""
        text = content.strip()
        if text.startswith("```"):
            text = re.sub(r"^```(?:json)?\s*", "", text)
            text = re.sub(r"\s*```$", "", text)

        # 只截取首尾大括号之间的内容，兼容模型多输出少量说明的情况。
        start_index = text.find("{")
        end_index = text.rfind("}")
        if start_index < 0 or end_index < start_index:
            raise ValueError("未找到 JSON 对象")
        data = json.loads(text[start_index : end_index + 1])
        if not isinstance(data, dict):
            raise ValueError("JSON 根节点不是对象")
        return data

    def _normalize_score(self, value: Any) -> int:
        """规范化大模型返回的分数。"""
        try:
            score = int(value)
        except (TypeError, ValueError) as exc:
            raise ValueError("score 不是有效整数") from exc
        return min(100, max(0, score))

    def _normalize_bool(self, value: Any, default: bool) -> bool:
        """规范化大模型返回的布尔值。"""
        if isinstance(value, bool):
            return value
        if isinstance(value, str):
            return value.strip().lower() in {"true", "1", "yes", "是", "正确"}
        return default

    def _normalize_string_list(self, value: Any) -> list[str]:
        """规范化大模型返回的字符串列表。"""
        if not isinstance(value, list):
            return []

        # 去除空白项，并限制单项长度，避免异常长文本影响前端展示。
        result: list[str] = []
        for item in value:
            text = str(item).strip()
            if text:
                result.append(text[:120])
        return result

    def _new_trace_id(self) -> str:
        """生成单次大模型调用追踪 ID。"""
        return uuid.uuid4().hex[:12]


practice_agent_service = PracticeAgentService()
