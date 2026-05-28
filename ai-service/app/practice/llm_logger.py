from __future__ import annotations

import json
import logging
from typing import Any

from langchain_core.messages import BaseMessage
from pydantic import BaseModel

from app.config.log_config import configure_logger
from app.config.settings import settings
from app.practice.provider_adapter import PracticeProviderAdapter
from app.schemas.practice import PracticeModelConfig
from app.time_utils import elapsed_milliseconds


logger = configure_logger("ai_service.practice.llm")


class PracticeLogSanitizer:
    """转换和脱敏大模型日志载荷。"""

    def __init__(self, provider_adapter: PracticeProviderAdapter) -> None:
        """初始化日志脱敏器依赖。"""
        self._provider_adapter = provider_adapter

    def message_to_log_payload(self, message: BaseMessage) -> dict[str, Any]:
        """把 LangChain 消息转换为可读日志结构。"""
        return {
            "type": getattr(message, "type", message.__class__.__name__),
            "content": self._provider_adapter.normalize_content(getattr(message, "content", "")),
            "additionalKwargs": self.to_log_payload(getattr(message, "additional_kwargs", {})),
        }

    def to_pretty_json(self, value: Any) -> str:
        """使用 UTF-8 友好的 JSON 字符串输出日志。"""
        return json.dumps(self.to_log_payload(value), ensure_ascii=False, indent=2, default=str)

    def to_log_payload(self, value: Any) -> Any:
        """递归转换复杂对象，确保日志不会因为不可序列化对象失败。"""
        if isinstance(value, BaseMessage):
            return self.message_to_log_payload(value)
        if isinstance(value, BaseModel):
            return value.model_dump(mode="json")
        if isinstance(value, dict):
            return {str(key): self.to_log_payload(item) for key, item in value.items()}
        if isinstance(value, (list, tuple, set)):
            return [self.to_log_payload(item) for item in value]
        return value


class PracticeLlmLogger:
    """封装 AI 智能刷题大模型调用日志策略。"""

    def __init__(self, provider_adapter: PracticeProviderAdapter, sanitizer: PracticeLogSanitizer) -> None:
        """初始化日志组件依赖。"""
        self._provider_adapter = provider_adapter
        self._sanitizer = sanitizer

    def log_request(
        self,
        trace_id: str,
        scene: str,
        system_prompt: str | None,
        messages: list[BaseMessage],
        stream: bool,
        model_config: PracticeModelConfig | None = None,
    ) -> None:
        """以 DEBUG 级别记录大模型调用入参。"""
        if not logger.isEnabledFor(logging.DEBUG):
            return

        # 用户问题、答案、题目和提示词只允许在开发调试时通过 DEBUG 输出。
        payload = {
            "scene": scene,
            "stream": stream,
            "model": self._provider_adapter.model_name(model_config),
            "modelProvider": settings.ai_grading_model_provider,
            "baseUrl": (
                self._provider_adapter.normalized_base_url(model_config)
                if self._provider_adapter.base_url(model_config)
                else ""
            ),
            "timeoutSeconds": settings.ai_grading_timeout_seconds,
            "maxOutputTokens": settings.ai_grading_max_output_tokens,
            "systemPrompt": system_prompt,
            "messages": [self._sanitizer.message_to_log_payload(message) for message in messages],
        }
        logger.debug(
            "【AI智能刷题-大模型调用入参】traceId=%s payload=%s",
            trace_id,
            self._sanitizer.to_pretty_json(payload),
        )

    def log_response(self, trace_id: str, scene: str, response: Any, elapsed_ms: int) -> None:
        """以 DEBUG 级别记录大模型调用返回。"""
        if not logger.isEnabledFor(logging.DEBUG):
            return

        # 模型回复和业务上下文只允许在开发调试时通过 DEBUG 输出。
        payload = {
            "scene": scene,
            "durationMs": elapsed_ms,
            "response": self._sanitizer.to_log_payload(response),
        }
        logger.debug("【AI智能刷题-大模型调用返回】traceId=%s payload=%s", trace_id, self._sanitizer.to_pretty_json(payload))

    def log_visible_stream_chunk(self, trace_id: str, start_time: float, source: str, count: int, content: str) -> None:
        """记录可见流式片段输出情况。"""
        if count != 1 and count % 50 != 0:
            return

        # 日志只打印长度和耗时，避免用户答案或模型全文进入日志。
        elapsed_ms = elapsed_milliseconds(start_time)
        logger.info(
            "【AI智能刷题流程-流式讨论】可见流式片段进度：traceId=%s source=%s count=%s chars=%s elapsedMs=%s",
            trace_id,
            source,
            count,
            len(content),
            elapsed_ms,
        )

    def log_stream_output_tokens(self, trace_id: str, output_tokens: int | None, full_reply: str) -> None:
        """记录流式讨论总输出 Token，便于核验 max_completion_tokens 是否生效。"""
        if output_tokens is not None:
            logger.info(
                "【AI智能刷题流程-流式讨论】Agent 流式讨论总输出Token：traceId=%s outputTokens=%s tokenSource=provider",
                trace_id,
                output_tokens,
            )
            return

        # 保留 full_reply 参数，便于后续日志策略需要估算 token 时扩展。
        _ = full_reply
        logger.info(
            "【AI智能刷题流程-流式讨论】Agent 流式讨论总输出Token：traceId=%s outputTokens=不可用 tokenSource=unavailable",
            trace_id,
        )
