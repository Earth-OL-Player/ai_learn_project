from pydantic import BaseModel, Field, model_validator


FULL_SCORE = 100


class PracticeModelConfig(BaseModel):
    """请求级模型配置。"""

    model: str = ""
    baseUrl: str = ""
    apiKey: str = ""
    configFingerprint: str = ""


class PracticeGradeRequest(BaseModel):
    """刷题答案评分请求。"""

    userId: str
    questionCode: str
    question: str
    questionType: str
    standardAnswer: str
    userAnswer: str
    modelConfig: PracticeModelConfig | None = None


class PracticeGradeEvaluation(BaseModel):
    """大模型答案评分结构化输出。"""

    score: int = Field(ge=0, le=100, description="百分制得分，必须是 0 到 100 的整数。")
    hitPoints: list[str] = Field(description="必填字段。用户答案命中的关键点，使用简短中文短句；没有命中时返回空数组。")
    missingPoints: list[str] = Field(description="必填字段。用户答案缺失的关键点，使用简短中文短句；没有缺失时返回空数组。")
    problems: list[str] = Field(description="必填字段。用户答案存在的表达、逻辑或概念问题，一到三句话；没有问题时返回空数组。")
    improvementAdvice: str = Field(description="基于 missingPoints 和 problems 给出最优先的改进建议；要求具体、可操作，不要空泛鼓励；一到三句话。")

    @model_validator(mode="after")
    def validate_explanation_points(self) -> "PracticeGradeEvaluation":
        """校验非满分评分必须给出可解释的扣分依据。"""
        if self.score < FULL_SCORE and not self.missingPoints and not self.problems:
            raise ValueError("非满分评分必须填写 missingPoints 或 problems")
        return self


class PracticeGradeResponse(PracticeGradeEvaluation):
    """刷题答案评分响应。"""

    referenceAnswer: str = Field(description="AI 服务从请求原文直接回填的参考答案，不要求大模型生成。")


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
    modelConfig: PracticeModelConfig | None = None


class PracticeDiscussResponse(BaseModel):
    """本题讨论响应。"""

    reply: str
