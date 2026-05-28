import logging

from app.config.settings import settings


_LOG_FORMAT = "%(asctime)s %(levelname)s [%(name)s] %(message)s"
_DEFAULT_LOG_LEVEL = logging.INFO
_SUPPORTED_LOG_LEVELS = {
    "TRACE": logging.DEBUG,
    "DEBUG": logging.DEBUG,
    "INFO": logging.INFO,
    "WARN": logging.WARNING,
    "ERROR": logging.ERROR,
}
_PROJECT_LOGGER_PREFIXES = ("ai_service", "app")
_current_log_level_name = "INFO"


def configure_logger(name: str) -> logging.Logger:
    """按 AI 服务统一配置创建日志记录器。"""
    logger = logging.getLogger(name)
    logger.setLevel(_resolve_log_level(_current_log_level_name))

    # 同一个 logger 只挂载一次控制台处理器，避免模块重复导入时日志重复输出。
    if not logger.handlers:
        stream_handler = logging.StreamHandler()
        stream_handler.setFormatter(logging.Formatter(_LOG_FORMAT))
        logger.addHandler(stream_handler)
    _apply_handler_level(logger, _resolve_log_level(_current_log_level_name))
    logger.propagate = False
    return logger


def get_service_log_level() -> str:
    """读取 AI 服务当前运行期日志级别。"""
    return _current_log_level_name


def set_service_log_level(level_name: str) -> str:
    """设置 AI 服务当前运行期日志级别。"""
    global _current_log_level_name

    normalized_level = _normalize_log_level_name(level_name, strict=True)
    log_level = _resolve_log_level(normalized_level)
    _current_log_level_name = normalized_level

    # 同步根日志器和已创建的项目日志器，保证运行期立即生效。
    logging.getLogger().setLevel(log_level)
    _apply_existing_logger_levels(log_level)
    return _current_log_level_name


def _resolve_log_level(level_name: str) -> int:
    """把环境变量中的日志级别转换为 logging 模块级别。"""
    normalized_level = _normalize_log_level_name(level_name)
    level = _SUPPORTED_LOG_LEVELS.get(normalized_level)
    if level is not None:
        return level

    # 无效配置按生产安全默认值处理，避免误开 DEBUG 输出敏感上下文。
    return _DEFAULT_LOG_LEVEL


def _normalize_log_level_name(level_name: str, strict: bool = False) -> str:
    """规整并校验日志级别名称。"""
    if not level_name or not level_name.strip():
        if strict:
            raise ValueError("日志级别不能为空")
        return logging.getLevelName(_DEFAULT_LOG_LEVEL)

    normalized_level = level_name.strip().upper()
    if normalized_level == "WARNING":
        return "WARN"
    if normalized_level in _SUPPORTED_LOG_LEVELS:
        return normalized_level
    if strict:
        raise ValueError("日志级别不支持")
    return logging.getLevelName(_DEFAULT_LOG_LEVEL)


def _apply_existing_logger_levels(log_level: int) -> None:
    """更新已经创建的项目日志器级别。"""
    for logger_name, logger_item in logging.Logger.manager.loggerDict.items():
        if not isinstance(logger_item, logging.Logger) or not _is_project_logger(logger_name):
            continue

        # 已经创建的 logger 需要同步更新，否则会继续使用旧级别。
        logger_item.setLevel(log_level)
        _apply_handler_level(logger_item, log_level)


def _apply_handler_level(logger: logging.Logger, log_level: int) -> None:
    """更新日志处理器级别。"""
    for handler in logger.handlers:
        handler.setLevel(log_level)


def _is_project_logger(logger_name: str) -> bool:
    """判断是否为 AI 服务项目内日志器。"""
    return logger_name.startswith(_PROJECT_LOGGER_PREFIXES)


# 模块加载完成后再解析环境变量，避免函数定义前被调用。
_current_log_level_name = _normalize_log_level_name(settings.ai_service_log_level)
