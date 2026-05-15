from pydantic import BaseModel, Field


class RagIndexTaskRequest(BaseModel):
    """RAG 入库任务请求。"""

    sourceType: str = "QUESTION"
    documents: list[dict] = Field(default_factory=list)


class RagIndexTaskResponse(BaseModel):
    """RAG 入库任务响应。"""

    taskId: str
    status: str
    message: str


class RagSearchRequest(BaseModel):
    """RAG 检索请求。"""

    query: str
    topK: int = 5


class RagSearchSnippet(BaseModel):
    """RAG 检索片段。"""

    sourceType: str
    sourceId: str
    title: str
    chunkText: str
    score: float
