from pydantic import BaseModel


class LogLevelRequest(BaseModel):
    """日志级别更新请求。"""

    level: str


class LogLevelResponse(BaseModel):
    """日志级别响应。"""

    level: str
    message: str
