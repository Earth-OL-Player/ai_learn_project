from collections.abc import Iterator
from dataclasses import dataclass

from app.practice.discussion_agent import PracticeDiscussionAgent
from app.practice.grading_agent import PracticeGradingAgent
from app.practice.llm_logger import PracticeLlmLogger, PracticeLogSanitizer, logger
from app.practice.local_fallback import PracticeLocalFallback
from app.practice.model_factory import PracticeModelFactory
from app.practice.prompts import PracticePromptBuilder
from app.practice.provider_adapter import PracticeProviderAdapter
from app.practice.sse import PracticeSseEncoder
from app.schemas.practice import (
    PracticeAiCallMetrics,
    PracticeDiscussRequest,
    PracticeGradeRequest,
    PracticeGradeResponse,
)


@dataclass
class PracticeGradeServiceResult:
    """AI 服务评分结果和观测指标。"""

    grading: PracticeGradeResponse | None
    metrics: PracticeAiCallMetrics


class PracticeAgentService:
    """AI 智能刷题 Agent 服务。"""

    def __init__(
        self,
        provider_adapter: PracticeProviderAdapter | None = None,
        grading_agent: PracticeGradingAgent | None = None,
        discussion_agent: PracticeDiscussionAgent | None = None,
        local_fallback: PracticeLocalFallback | None = None,
        sse_encoder: PracticeSseEncoder | None = None,
    ) -> None:
        """初始化刷题 Agent 服务门面。"""
        self._provider_adapter = provider_adapter or PracticeProviderAdapter()
        prompt_builder = PracticePromptBuilder()
        self._model_factory = PracticeModelFactory(self._provider_adapter)
        sanitizer = PracticeLogSanitizer(self._provider_adapter)
        llm_logger = PracticeLlmLogger(self._provider_adapter, sanitizer)

        # 默认依赖在门面内装配，外部接口保持原有调用方式。
        self._local_fallback = local_fallback or PracticeLocalFallback()
        self._sse_encoder = sse_encoder or PracticeSseEncoder()
        self._grading_agent = grading_agent or PracticeGradingAgent(
            self._model_factory,
            prompt_builder,
            llm_logger,
            self._provider_adapter,
        )
        self._discussion_agent = discussion_agent or PracticeDiscussionAgent(
            self._model_factory,
            prompt_builder,
            llm_logger,
            self._provider_adapter,
            self._local_fallback,
            self._sse_encoder,
        )

    def grade_answer(self, request: PracticeGradeRequest, trace_id: str) -> PracticeGradeServiceResult:
        """调用真实 Agent 评分，失败时交由 Java 后端本地兜底。"""
        model_config = request.modelConfig
        llm_enabled = self._provider_adapter.is_llm_enabled(model_config)
        logger.info(
            "【AI智能刷题流程-评分】收到答案评分请求：traceId=%s userId=%s questionCode=%s llmEnabled=%s",
            trace_id,
            request.userId,
            request.questionCode,
            llm_enabled,
        )

        if llm_enabled:
            return self._grading_agent.grade_answer(request, trace_id)

        model_name = self._provider_adapter.model_name(model_config)
        base_url_configured = bool(self._provider_adapter.base_url(model_config))
        logger.info(
            "【AI智能刷题流程-评分】未启用真实 Agent，返回失败并交由 Java 后端本地兜底：traceId=%s model=%s baseUrlConfigured=%s",
            trace_id,
            model_name,
            base_url_configured,
        )
        metrics = PracticeAiCallMetrics(
            traceId=trace_id,
            scene="practice_grade",
            model=model_name,
            modelProvider=self._provider_adapter.model_provider(),
            success=False,
            fallbackUsed=True,
            durationMs=0,
            errorCategory="MODEL_DISABLED",
        )
        return PracticeGradeServiceResult(grading=None, metrics=metrics)

    def stream_discuss(self, request: PracticeDiscussRequest, trace_id: str) -> Iterator[str]:
        """流式生成本题讨论回复。"""
        model_config = request.modelConfig
        llm_enabled = self._provider_adapter.is_llm_enabled(model_config)
        logger.info(
            "【AI智能刷题流程-流式讨论】收到流式讨论请求：traceId=%s questionCode=%s historySize=%s llmEnabled=%s",
            trace_id,
            request.questionCode,
            len(request.conversationHistory),
            llm_enabled,
        )

        # 流式接口只在最终完成时打印汇总结果，避免 token 级日志刷屏。
        if llm_enabled:
            yield from self._discussion_agent.stream_discuss(request, trace_id)
            return

        # 未启用真实模型时仍返回 SSE，保证 Java 后端和前端链路稳定。
        model_name = self._provider_adapter.model_name(model_config)
        base_url_configured = bool(self._provider_adapter.base_url(model_config))
        logger.info(
            "【AI智能刷题流程-流式讨论】未调用真实 Agent，流式返回讨论不可用提示：traceId=%s model=%s baseUrlConfigured=%s",
            trace_id,
            model_name,
            base_url_configured,
        )
        fallback_response = self._local_fallback.discuss(request)
        yield from self._sse_encoder.complete_message(fallback_response.reply)

    def clear_cached_model_objects(self) -> int:
        """清理模型和 Agent 构造缓存。"""
        cleared_count = self._model_factory.clear_cached_objects()
        logger.info("Python AI 服务模型和 Agent 缓存已清理：clearedObjects=%s", cleared_count)
        return cleared_count


practice_agent_service = PracticeAgentService()
