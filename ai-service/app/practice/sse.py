from __future__ import annotations

import json
from typing import Any


class PracticeSseEncoder:
    """构造 AI 智能刷题内部 SSE 事件。"""

    def build_event(self, event: str, data: dict[str, Any]) -> str:
        """构造内部 SSE 事件文本。"""
        payload = json.dumps(data, ensure_ascii=False)
        return f"event: {event}\ndata: {payload}\n\n"

    def message(self, content: str) -> str:
        """构造消息片段事件。"""
        return self.build_event("message", {"content": content})

    def complete_message(self, content: str) -> tuple[str, str]:
        """构造单条消息后立即完成的事件序列。"""
        # 本地兜底和异常兜底都必须先发送内容再发送完成事件。
        return self.message(content), self.done()

    def done(self) -> str:
        """构造流式完成事件。"""
        return self.build_event("done", {})
