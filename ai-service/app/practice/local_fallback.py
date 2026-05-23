from __future__ import annotations

from app.schemas.practice import PracticeDiscussRequest, PracticeDiscussResponse


# 本地讨论兜底固定回复，避免模型异常时伪造 AI 追问能力。
FALLBACK_DISCUSS_REPLY = "抱歉，当前大模型调用异常，仅保留兜底策略评分功能，无法和您进行探讨。"


class PracticeLocalFallback:
    """封装 AI 智能刷题本地兜底策略。"""

    def discuss(self, request: PracticeDiscussRequest) -> PracticeDiscussResponse:
        """大模型异常时返回无法继续探讨的兜底提示。"""
        _ = request

        # 讨论能力不再使用本地规则伪造，避免给用户造成模型仍可追问的误解。
        return PracticeDiscussResponse(reply=FALLBACK_DISCUSS_REPLY)
