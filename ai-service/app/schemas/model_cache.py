from pydantic import BaseModel


class ModelCacheInvalidationResponse(BaseModel):
    """模型缓存失效响应。"""

    clearedObjects: int
    message: str
