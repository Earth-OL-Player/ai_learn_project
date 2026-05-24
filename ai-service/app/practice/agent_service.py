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
        model_factory = PracticeModelFactory(self._provider_adapter)
        sanitizer = PracticeLogSanitizer(self._provider_adapter)
        llm_logger = PracticeLlmLogger(self._provider_adapter, sanitizer)

        # 默认依赖在门面内装配，外部接口保持原有调用方式。
        self._local_fallback = local_fallback or PracticeLocalFallback()
        self._sse_encoder = sse_encoder or PracticeSseEncoder()
        self._grading_agent = grading_agent or PracticeGradingAgent(model_factory, prompt_builder, llm_logger, self._provider_adapter)
        self._discussion_agent = discussion_agent or PracticeDiscussionAgent(
            model_factory,
            prompt_builder,
            llm_logger,
            self._provider_adapter,
            self._local_fallback,
            self._sse_encoder,
        )

    def grade_answer(self, request: PracticeGradeRequest, trace_id: str) -> PracticeGradeServiceResult:
        """调用真实 Agent 评分，失败时交由 Java 后端本地兜底。"""
        logger.info(
            "【AI智能刷题流程-评分】收到答案评分请求：traceId=%s userId=%s questionCode=%s llmEnabled=%s",
            trace_id,
            request.userId,
            request.questionCode,
            self._provider_adapter.is_llm_enabled(request.modelConfig),
        )

        if self._provider_adapter.is_llm_enabled(request.modelConfig):
            return self._grading_agent.grade_answer(request, trace_id)

        logger.info(
            "【AI智能刷题流程-评分】未启用真实 Agent，返回失败并交由 Java 后端本地兜底：traceId=%s model=%s baseUrlConfigured=%s",
            trace_id,
            self._provider_adapter.model_name(request.modelConfig),
            bool(self._provider_adapter.base_url(request.modelConfig)),
        )
        metrics = PracticeAiCallMetrics(
            traceId=trace_id,
            scene="practice_grade",
            model=self._provider_adapter.model_name(request.modelConfig),
            modelProvider=self._provider_adapter.model_provider(),
            success=False,
            fallbackUsed=True,
            durationMs=0,
            errorCategory="MODEL_DISABLED",
        )
        return PracticeGradeServiceResult(grading=None, metrics=metrics)

    def stream_discuss(self, request: PracticeDiscussRequest, trace_id: str) -> Iterator[str]:
        """流式生成本题讨论回复。"""
        logger.info(
            "【AI智能刷题流程-流式讨论】收到流式讨论请求：traceId=%s questionCode=%s historySize=%s llmEnabled=%s",
            trace_id,
            request.questionCode,
            len(request.conversationHistory),
            self._provider_adapter.is_llm_enabled(request.modelConfig),
        )

        # 流式接口只在最终完成时打印汇总结果，避免 token 级日志刷屏。
        if self._provider_adapter.is_llm_enabled(request.modelConfig):
            yield from self._discussion_agent.stream_discuss(request, trace_id)
            return

        # 未启用真实模型时仍返回 SSE，保证 Java 后端和前端链路稳定。
        logger.info(
            "【AI智能刷题流程-流式讨论】未调用真实 Agent，流式返回讨论不可用提示：traceId=%s model=%s baseUrlConfigured=%s",
            trace_id,
            self._provider_adapter.model_name(request.modelConfig),
            bool(self._provider_adapter.base_url(request.modelConfig)),
        )
        fallback_response = self._local_fallback.discuss(request)
        yield self._sse_encoder.message(fallback_response.reply)
        yield self._sse_encoder.done()


practice_agent_service = PracticeAgentService()
