from typing import Generic, TypeVar

from pydantic import BaseModel, Field

T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    """统一响应结构。"""

    code: str = "SUCCESS"
    message: str = "操作成功"
    data: T | None = None
    traceId: str = "ai-service-trace"


class GradeAnswerRequest(BaseModel):
    """答案评分请求。"""

    userId: str
    questionId: str
    questionContent: str
    standardAnswer: str
    userAnswer: str
    knowledgePoints: list[str] = Field(default_factory=list)
    contextSnippets: list[str] = Field(default_factory=list)


class GradeAnswerResponse(BaseModel):
    """答案评分响应。"""

    score: int
    isCorrect: bool
    hitPoints: list[str]
    missingPoints: list[str]
    problems: list[str]
    referenceAnswer: str
    improvementAdvice: str
    reviewKnowledgePoints: list[str]
