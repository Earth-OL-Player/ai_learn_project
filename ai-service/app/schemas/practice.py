from pydantic import BaseModel, Field


class PracticeGradeRequest(BaseModel):
    """刷题答案评分请求。"""

    userId: str
    questionCode: str
    question: str
    questionType: str
    standardAnswer: str
    userAnswer: str


class PracticeGradeResponse(BaseModel):
    """刷题答案评分响应。"""

    score: int = Field(ge=0, le=100, description="百分制得分，必须是 0 到 100 的整数。")
    hitPoints: list[str] = Field(default_factory=list, description="用户答案命中的关键点，使用简短中文短句。")
    missingPoints: list[str] = Field(default_factory=list, description="用户答案缺失的关键点，使用简短中文短句。")
    problems: list[str] = Field(default_factory=list, description="用户答案存在的表达、逻辑或概念问题，一到三句话。")
    referenceAnswer: str = Field(description="参考答案原文。")
    improvementAdvice: str = Field(description="基于 missingPoints 和 problems 给出最优先的改进建议；要求具体、可操作，不要空泛鼓励；一到三句话。")


class PracticeAiCallMetrics(BaseModel):
    """AI 调用观测指标。"""

    traceId: str
    scene: str
    model: str
    modelProvider: str = ""
    success: bool
    fallbackUsed: bool = False
    stream: bool = False
    firstTokenMs: int | None = None
    durationMs: int
    inputTokens: int | None = None
    outputTokens: int | None = None
    totalTokens: int | None = None
    estimatedCost: str = "unavailable"
    errorCategory: str = ""


class PracticeConversationMessage(BaseModel):
    """当前题短期讨论历史消息。"""

    role: str
    content: str


class PracticeDiscussRequest(BaseModel):
    """本题讨论请求。"""

    questionCode: str
    question: str
    questionType: str
    standardAnswer: str
    lastUserAnswer: str = ""
    gradingSummary: str = ""
    conversationHistory: list[PracticeConversationMessage] = Field(default_factory=list)
    message: str


class PracticeDiscussResponse(BaseModel):
    """本题讨论响应。"""

    reply: str
