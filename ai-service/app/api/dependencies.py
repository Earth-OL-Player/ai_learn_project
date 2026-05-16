import logging

from fastapi import Header, HTTPException, status

from app.config.settings import settings

logger = logging.getLogger("ai_service.auth")
logger.setLevel(logging.INFO)
if not logger.handlers:
    stream_handler = logging.StreamHandler()
    stream_handler.setFormatter(logging.Formatter("%(asctime)s %(levelname)s [%(name)s] %(message)s"))
    logger.addHandler(stream_handler)
logger.propagate = False


def verify_internal_token(x_internal_token: str | None = Header(default=None)) -> None:
    """校验内部调用 Token。"""
    if not x_internal_token or x_internal_token != settings.ai_service_token:
        logger.warning("Python AI 服务内部鉴权失败：hasToken=%s", bool(x_internal_token))
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="内部服务鉴权失败")
