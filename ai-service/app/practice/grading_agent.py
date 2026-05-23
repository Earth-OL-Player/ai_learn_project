from __future__ import annotations

import time
import uuid
from typing import Any

from app.config.settings import settings
from app.practice.llm_logger import PracticeLlmLogger, logger
from app.practice.model_factory import PracticeModelFactory
from app.practice.prompts import GRADE_SYSTEM_PROMPT, PracticePromptBuilder
from app.schemas.practice import PracticeGradeRequest, PracticeGradeResponse


class PracticeGradingAgent:
    """负责 AI 智能刷题答案评分。"""

    def __init__(self, model_factory: PracticeModelFactory, prompt_builder: PracticePromptBuilder, llm_logger: PracticeLlmLogger) -> None:
        """初始化评分 Agent 依赖。"""
        self._model_factory = model_factory
        self._prompt_builder = prompt_builder
        self._llm_logger = llm_logger

    def grade_answer(self, request: PracticeGradeRequest) -> PracticeGradeResponse | None:
        """使用 LangChain Agent 非流式完成答案评分。"""
        trace_id = self._new_trace_id()
        messages = self._prompt_builder.build_grade_messages(request)
        start_time = time.perf_counter()
        logger.info(
            "【AI智能刷题流程-评分】准备调用 Agent 结构化评分：traceId=%s model=%s",
            trace_id,
            settings.ai_grading_model,
        )
        self._llm_logger.log_request(trace_id, "答案评分-Agent非流式", GRADE_SYSTEM_PROMPT, messages, stream=False)
        try:
            result = self._model_factory.grading_agent().invoke({"messages": messages})
            grading = self._structured_grading_result(result)
            if grading is None:
                raise ValueError("Agent 未返回结构化评分结果")

            # LangChain Agent 结构化输出成功后记录完整评分结果，便于按 traceId 复盘返回。
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            self._llm_logger.log_response(trace_id, "答案评分-Agent非流式", {"grading": grading, "rawResult": result}, elapsed_ms)
            logger.info(
                "【AI智能刷题流程-评分】Agent 结构化评分完成：traceId=%s model=%s durationMs=%s score=%s",
                trace_id,
                settings.ai_grading_model,
                elapsed_ms,
                grading.score,
            )
            return grading
        except Exception as exc:  # noqa: BLE001 - Agent、网络和结构化解析异常统一交由 Java 后端兜底。
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            logger.warning(
                "【AI智能刷题流程-评分】Agent 结构化评分失败，交由 Java 后端本地兜底：traceId=%s durationMs=%s error=%s",
                trace_id,
                elapsed_ms,
                exc,
                exc_info=True,
            )
            return None

    def _structured_grading_result(self, result: Any) -> PracticeGradeResponse | None:
        """从 Agent 执行结果中读取结构化评分结果。"""
        if isinstance(result, PracticeGradeResponse):
            return result
        if not isinstance(result, dict):
            return None

        # LangChain Agent response_format 成功时会返回 structured_response。
        structured_response = result.get("structured_response")
        if structured_response is not None:
            return PracticeGradeResponse.model_validate(structured_response)

        # 兼容结构化结果落在 tool_calls 中的场景，避免供应商返回差异导致评分丢失。
        messages = result.get("messages")
        if isinstance(messages, list):
            return self._grading_result_from_tool_calls(messages)
        return None

    def _grading_result_from_tool_calls(self, messages: list[Any]) -> PracticeGradeResponse | None:
        """从 Agent 工具调用消息中读取结构化评分结果。"""
        for message in reversed(messages):
            tool_calls = getattr(message, "tool_calls", None)
            if not tool_calls:
                continue

            # Agent 结构化输出会以模型工具调用形式携带 Pydantic 参数。
            for tool_call in tool_calls:
                if not isinstance(tool_call, dict):
                    continue
                if tool_call.get("name") == PracticeGradeResponse.__name__ and tool_call.get("args"):
                    return PracticeGradeResponse.model_validate(tool_call["args"])
        return None

    def _new_trace_id(self) -> str:
        """生成单次大模型调用追踪 ID。"""
        return uuid.uuid4().hex[:12]
