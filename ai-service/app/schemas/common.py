from typing import Generic, TypeVar

from pydantic import BaseModel

T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    """统一响应结构。"""

    code: str = "SUCCESS"
    message: str = "操作成功"
    data: T | None = None
    traceId: str = "ai-service-trace"
