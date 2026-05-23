import logging

from app.config.settings import settings


_LOG_FORMAT = "%(asctime)s %(levelname)s [%(name)s] %(message)s"
_DEFAULT_LOG_LEVEL = logging.INFO


def configure_logger(name: str) -> logging.Logger:
    """按 AI 服务统一配置创建日志记录器。"""
    logger = logging.getLogger(name)
    logger.setLevel(_resolve_log_level(settings.ai_service_log_level))

    # 同一个 logger 只挂载一次控制台处理器，避免模块重复导入时日志重复输出。
    if not logger.handlers:
        stream_handler = logging.StreamHandler()
        stream_handler.setFormatter(logging.Formatter(_LOG_FORMAT))
        logger.addHandler(stream_handler)
    logger.propagate = False
    return logger


def _resolve_log_level(level_name: str) -> int:
    """把环境变量中的日志级别转换为 logging 模块级别。"""
    level = getattr(logging, level_name.strip().upper(), None)
    if isinstance(level, int):
        return level

    # 无效配置按生产安全默认值处理，避免误开 DEBUG 输出敏感上下文。
    return _DEFAULT_LOG_LEVEL
