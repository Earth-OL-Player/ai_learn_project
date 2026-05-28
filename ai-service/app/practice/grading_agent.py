from __future__ import annotations

from dataclasses import dataclass
import time
from typing import Any

from app.practice.llm_logger import PracticeLlmLogger, logger
from app.practice.model_factory import PracticeModelFactory
from app.practice.prompts import GRADE_SYSTEM_PROMPT, PracticePromptBuilder
from app.practice.provider_adapter import PracticeProviderAdapter
from app.schemas.practice import PracticeAiCallMetrics, PracticeGradeEvaluation, PracticeGradeRequest, PracticeGradeResponse
from app.time_utils import elapsed_milliseconds


@dataclass
class PracticeGradeAgentResult:
    """评分 Agent 结果和观测指标。"""

    grading: PracticeGradeResponse | None
    metrics: PracticeAiCallMetrics


class PracticeGradingAgent:
    """负责 AI 智能刷题答案评分。"""

    def __init__(
        self,
        model_factory: PracticeModelFactory,
        prompt_builder: PracticePromptBuilder,
        llm_logger: PracticeLlmLogger,
        provider_adapter: PracticeProviderAdapter,
    ) -> None:
        """初始化评分 Agent 依赖。"""
        self._model_factory = model_factory
        self._prompt_builder = prompt_builder
        self._llm_logger = llm_logger
        self._provider_adapter = provider_adapter

    def grade_answer(self, request: PracticeGradeRequest, trace_id: str) -> PracticeGradeAgentResult:
        """使用 LangChain Agent 非流式完成答案评分。"""
        messages = self._prompt_builder.build_grade_messages(request)
        start_time = time.perf_counter()
        logger.info(
            "【AI智能刷题流程-评分】准备调用 Agent 结构化评分：traceId=%s model=%s",
            trace_id,
            self._provider_adapter.model_name(request.modelConfig),
        )
        self._llm_logger.log_request(
            trace_id,
            "答案评分-Agent非流式",
            GRADE_SYSTEM_PROMPT,
            messages,
            stream=False,
            model_config=request.modelConfig,
        )
        try:
            result = self._model_factory.grading_agent(request.modelConfig).invoke({"messages": messages})
            grading = self._structured_grading_result(result, request.standardAnswer)
            if grading is None:
                raise ValueError("Agent 未返回结构化评分结果")

            # LangChain Agent 结构化输出成功后记录完整评分结果，便于按 traceId 复盘返回。
            elapsed_ms = elapsed_milliseconds(start_time)
            self._llm_logger.log_response(trace_id, "答案评分-Agent非流式", {"grading": grading, "rawResult": result}, elapsed_ms)
            metrics = self._build_metrics(trace_id, elapsed_ms, True, "", result, request)
            logger.info(
                "【AI智能刷题流程-评分】Agent 结构化评分完成：traceId=%s model=%s durationMs=%s score=%s inputTokens=%s outputTokens=%s totalTokens=%s estimatedCost=%s",
                trace_id,
                self._provider_adapter.model_name(request.modelConfig),
                elapsed_ms,
                grading.score,
                metrics.inputTokens,
                metrics.outputTokens,
                metrics.totalTokens,
                metrics.estimatedCost,
            )
            return PracticeGradeAgentResult(grading=grading, metrics=metrics)
        except Exception as exc:  # noqa: BLE001 - Agent、网络和结构化解析异常统一交由 Java 后端兜底。
            elapsed_ms = elapsed_milliseconds(start_time)
            metrics = self._build_metrics(trace_id, elapsed_ms, False, self._error_category(exc), None, request)
            logger.warning(
                "【AI智能刷题流程-评分】Agent 结构化评分失败，交由 Java 后端本地兜底：traceId=%s durationMs=%s errorCategory=%s error=%s",
                trace_id,
                elapsed_ms,
                metrics.errorCategory,
                exc,
                exc_info=True,
            )
            return PracticeGradeAgentResult(grading=None, metrics=metrics)

    def _structured_grading_result(self, result: Any, reference_answer: str) -> PracticeGradeResponse | None:
        """从 Agent 执行结果中读取结构化评分结果。"""
        if isinstance(result, PracticeGradeEvaluation):
            return self._build_grade_response(result, reference_answer)
        if not isinstance(result, dict):
            return None

        # LangChain Agent response_format 成功时会返回 structured_response。
        structured_response = result.get("structured_response")
        if structured_response is not None:
            evaluation = PracticeGradeEvaluation.model_validate(structured_response)
            return self._build_grade_response(evaluation, reference_answer)

        # 兼容结构化结果落在 tool_calls 中的场景，避免供应商返回差异导致评分丢失。
        messages = result.get("messages")
        if isinstance(messages, list):
            return self._grading_result_from_tool_calls(messages, reference_answer)
        return None

    def _grading_result_from_tool_calls(self, messages: list[Any], reference_answer: str) -> PracticeGradeResponse | None:
        """从 Agent 工具调用消息中读取结构化评分结果。"""
        for message in reversed(messages):
            tool_calls = getattr(message, "tool_calls", None)
            if not tool_calls:
                continue

            # Agent 结构化输出会以模型工具调用形式携带 Pydantic 参数。
            for tool_call in tool_calls:
                if not isinstance(tool_call, dict):
                    continue
                if tool_call.get("name") == PracticeGradeEvaluation.__name__ and tool_call.get("args"):
                    evaluation = PracticeGradeEvaluation.model_validate(tool_call["args"])
                    return self._build_grade_response(evaluation, reference_answer)
        return None

    def _build_grade_response(self, evaluation: PracticeGradeEvaluation, reference_answer: str) -> PracticeGradeResponse:
        """用模型评分结果和服务端参考答案组装接口响应。"""
        return PracticeGradeResponse(
            **evaluation.model_dump(mode="python"),
            referenceAnswer=reference_answer,
        )

    def _build_metrics(
        self,
        trace_id: str,
        elapsed_ms: int,
        success: bool,
        error_category: str,
        result: Any,
        request: PracticeGradeRequest,
    ) -> PracticeAiCallMetrics:
        """构造评分链路观测指标。"""
        token_usage = self._provider_adapter.call_token_usage(result)
        return PracticeAiCallMetrics(
            traceId=trace_id,
            scene="practice_grade",
            model=self._provider_adapter.model_name(request.modelConfig),
            modelProvider=self._provider_adapter.model_provider(),
            success=success,
            fallbackUsed=not success,
            stream=False,
            durationMs=elapsed_ms,
            inputTokens=token_usage["inputTokens"],
            outputTokens=token_usage["outputTokens"],
            totalTokens=token_usage["totalTokens"],
            errorCategory=error_category,
        )

    def _error_category(self, exc: Exception) -> str:
        """归类模型调用异常，便于后续计算超时率和失败率。"""
        error_name = exc.__class__.__name__.lower()
        if "timeout" in error_name:
            return "TIMEOUT"
        if isinstance(exc, ValueError):
            return "PARSE_ERROR"
        return "MODEL_ERROR"
