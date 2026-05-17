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

    score: int = Field(ge=0, le=100)
    correct: bool
    hitPoints: list[str] = Field(default_factory=list)
    missingPoints: list[str] = Field(default_factory=list)
    problems: list[str] = Field(default_factory=list)
    referenceAnswer: str
    improvementAdvice: str
    reviewKnowledgePoints: list[str] = Field(default_factory=list)
    fallbackUsed: bool = False


class PracticeDiscussRequest(BaseModel):
    """本题讨论请求。"""

    questionCode: str
    question: str
    questionType: str
    standardAnswer: str
    lastUserAnswer: str = ""
    message: str


class PracticeDiscussResponse(BaseModel):
    """本题讨论响应。"""

    reply: str


class PracticeRelevanceRequest(BaseModel):
    """刷题输入相关性判断请求。"""

    questionCode: str
    question: str
    questionType: str
    standardAnswer: str
    phase: str
    message: str


class PracticeRelevanceResponse(BaseModel):
    """刷题输入相关性判断响应。"""

    relevant: bool = True
    reason: str = ""
