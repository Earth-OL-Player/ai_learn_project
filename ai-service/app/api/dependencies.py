from fastapi import Header, HTTPException, status

from app.config.log_config import configure_logger
from app.config.settings import settings

logger = configure_logger("ai_service.auth")


def verify_internal_token(x_internal_token: str | None = Header(default=None)) -> None:
    """校验内部调用 Token。"""
    if not x_internal_token or x_internal_token != settings.ai_service_token:
        logger.warning("Python AI 服务内部鉴权失败：hasToken=%s", bool(x_internal_token))
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="内部服务鉴权失败")
