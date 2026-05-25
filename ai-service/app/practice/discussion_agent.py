from __future__ import annotations

import time
from collections.abc import Iterator
from dataclasses import dataclass

from langchain_core.messages import BaseMessage

from app.config.settings import settings
from app.practice.llm_logger import PracticeLlmLogger, logger
from app.practice.local_fallback import PracticeLocalFallback
from app.practice.model_factory import PracticeModelFactory
from app.practice.prompts import DISCUSSION_SYSTEM_PROMPT, PracticePromptBuilder
from app.practice.provider_adapter import PracticeProviderAdapter
from app.practice.sse import PracticeSseEncoder
from app.schemas.practice import PracticeDiscussRequest, PracticeDiscussResponse


@dataclass
class StreamDiscussChunk:
    """流式讨论单片段内容和供应商返回的 Token 用量。"""

    content: str
    output_tokens: int | None = None


class PracticeDiscussionAgent:
    """负责 AI 智能刷题本题讨论和流式输出。"""

    def __init__(
        self,
        model_factory: PracticeModelFactory,
        prompt_builder: PracticePromptBuilder,
        llm_logger: PracticeLlmLogger,
        provider_adapter: PracticeProviderAdapter,
        local_fallback: PracticeLocalFallback,
        sse_encoder: PracticeSseEncoder,
    ) -> None:
        """初始化讨论 Agent 依赖。"""
        self._model_factory = model_factory
        self._prompt_builder = prompt_builder
        self._llm_logger = llm_logger
        self._provider_adapter = provider_adapter
        self._local_fallback = local_fallback
        self._sse_encoder = sse_encoder

    def stream_discuss(self, request: PracticeDiscussRequest, trace_id: str) -> Iterator[str]:
        """使用 LangChain Agent 流式接口生成讨论回复。"""
        start_time = time.perf_counter()
        messages = self._prompt_builder.build_discuss_messages(request)
        logger.info(
            "【AI智能刷题流程-流式讨论】准备调用 Agent 流式讨论：traceId=%s model=%s",
            trace_id,
            self._provider_adapter.model_name(request.modelConfig),
        )
        try:
            yield from self._stream_discuss_by_agent(request, messages, trace_id, start_time)
        except Exception as exc:  # noqa: BLE001 - Agent 流式异常统一进入本地兜底。
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            logger.warning(
                "【AI智能刷题流程-流式讨论】Agent 流式讨论失败：traceId=%s durationMs=%s error=%s",
                trace_id,
                elapsed_ms,
                exc,
                exc_info=True,
            )
            fallback_response = self._local_fallback.discuss(request)
            yield self._sse_encoder.message(fallback_response.reply)
            yield self._sse_encoder.done()

    def generate_discuss_reply(self, request: PracticeDiscussRequest, trace_id: str) -> PracticeDiscussResponse | None:
        """使用 LangChain Agent 非流式为讨论链路兜底生成完整回复。"""
        start_time = time.perf_counter()
        messages = self._prompt_builder.build_discuss_messages(request)
        logger.info(
            "【AI智能刷题流程-讨论】准备调用 Agent 非流式讨论兜底：traceId=%s model=%s",
            trace_id,
            self._provider_adapter.model_name(request.modelConfig),
        )
        self._llm_logger.log_request(
            trace_id,
            "本题讨论-Agent非流式兜底",
            DISCUSSION_SYSTEM_PROMPT,
            messages,
            stream=False,
            model_config=request.modelConfig,
        )
        try:
            result = self._model_factory.discussion_agent(request.modelConfig).invoke({"messages": messages})
            reply = self._provider_adapter.last_ai_reply(result).strip()
            if not reply:
                return None

            # Agent 流式无可见片段时记录完整返回，排查时可直接看到最终回复内容。
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            self._llm_logger.log_response(trace_id, "本题讨论-Agent非流式兜底", {"reply": reply, "rawResult": result}, elapsed_ms)
            logger.info(
                "【AI智能刷题流程-讨论】Agent 非流式讨论兜底完成：traceId=%s durationMs=%s replyChars=%s",
                trace_id,
                elapsed_ms,
                len(reply),
            )
            return PracticeDiscussResponse(reply=reply)
        except Exception as exc:  # noqa: BLE001 - Agent 和图执行异常统一进入本地兜底。
            elapsed_ms = round((time.perf_counter() - start_time) * 1000)
            logger.warning(
                "【AI智能刷题流程-讨论】Agent 非流式讨论兜底失败，使用本地兜底：traceId=%s durationMs=%s error=%s",
                trace_id,
                elapsed_ms,
                exc,
                exc_info=True,
            )
            return None

    def _stream_discuss_by_agent(
        self,
        request: PracticeDiscussRequest,
        messages: list[BaseMessage],
        trace_id: str,
        start_time: float,
    ) -> Iterator[str]:
        """编排 Agent 流式输出和无输出兜底。"""
        emitted_any = False
        full_reply_parts: list[str] = []
        output_tokens: int | None = None

        # 讨论阶段统一优先使用 Agent 流式，便于后续接入工具、检索和多步骤规划。
        for stream_chunk in self._stream_discuss_with_agent(request, messages, trace_id, start_time):
            content = stream_chunk.content
            if stream_chunk.output_tokens is not None:
                output_tokens = stream_chunk.output_tokens
            if not content:
                continue
            emitted_any = True
            full_reply_parts.append(content)
            yield self._sse_encoder.message(content)

        # Agent 流式无输出时，再切换 Agent 非流式兜底，避免前端长时间空白。
        if not emitted_any:
            logger.warning("Agent 流式链路无可见片段，切换 Agent 非流式兜底：traceId=%s", trace_id)
            fallback_response = self.generate_discuss_reply(request, trace_id) or self._local_fallback.discuss(request)
            full_reply_parts.append(fallback_response.reply)
            yield self._sse_encoder.message(fallback_response.reply)
        yield self._sse_encoder.done()

        # 流式完成后只记录汇总结果，避免 token 级日志刷屏。
        elapsed_ms = round((time.perf_counter() - start_time) * 1000)
        full_reply = "".join(full_reply_parts)
        self._llm_logger.log_response(trace_id, "本题讨论-Agent流式汇总", {"reply": full_reply}, elapsed_ms)
        logger.info(
            "【AI智能刷题流程-流式讨论】Agent 流式讨论完成：traceId=%s durationMs=%s replyChars=%s maxOutputTokens=%s outputTokens=%s estimatedCost=%s",
            trace_id,
            elapsed_ms,
            len(full_reply),
            settings.ai_grading_max_output_tokens,
            output_tokens,
            "unavailable",
        )
        self._llm_logger.log_stream_output_tokens(trace_id, output_tokens, full_reply)

    def _stream_discuss_with_agent(
        self,
        request: PracticeDiscussRequest,
        messages: list[BaseMessage],
        trace_id: str,
        start_time: float,
    ) -> Iterator[StreamDiscussChunk]:
        """使用 LangChain Agent 流式输出讨论回复。"""
        event_count = 0
        content_count = 0
        self._llm_logger.log_request(
            trace_id,
            "本题讨论-Agent流式",
            DISCUSSION_SYSTEM_PROMPT,
            messages,
            stream=True,
            model_config=request.modelConfig,
        )
        for chunk in self._model_factory.discussion_agent(request.modelConfig).stream(
            {"messages": messages},
            stream_mode="messages",
            version="v2",
        ):
            event_count += 1
            content = self._provider_adapter.agent_stream_content(chunk)
            output_tokens = self._provider_adapter.stream_output_tokens(chunk)
            if content:
                content_count += 1
                self._llm_logger.log_visible_stream_chunk(trace_id, start_time, "agent", content_count, content)
                yield StreamDiscussChunk(content=content, output_tokens=output_tokens)
            elif output_tokens is not None:
                yield StreamDiscussChunk(content="", output_tokens=output_tokens)

        # 记录事件数和可见文本数，用于定位供应商是否支持 Agent token 流。
        logger.info("【AI智能刷题流程-流式讨论】Agent流式事件统计：traceId=%s events=%s visibleChunks=%s", trace_id, event_count, content_count)
