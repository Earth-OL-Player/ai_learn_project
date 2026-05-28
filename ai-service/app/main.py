import time
from collections.abc import Awaitable, Callable

from fastapi import FastAPI
from fastapi import Request
from fastapi.exceptions import RequestValidationError
from fastapi.exception_handlers import request_validation_exception_handler
from fastapi.responses import Response

from app.api.log_levels import router as log_levels_router
from app.api.model_cache import router as model_cache_router
from app.api.practice import router as practice_router
from app.config.log_config import configure_logger
from app.time_utils import elapsed_milliseconds

app = FastAPI(title="AI Learn Service", version="0.2.0")
logger = configure_logger("ai_service.http")


@app.middleware("http")
async def log_http_request(request: Request, call_next: Callable[[Request], Awaitable[Response]]) -> Response:
    """记录进入 Python AI 服务的 HTTP 请求，便于判断前端链路是否打到本服务。"""
    start_time = time.perf_counter()
    trace_id = request.headers.get("x-trace-id", "")

    # 请求进入时先打日志，避免鉴权失败或异常时看不到链路。
    logger.info("收到 Python AI 服务请求：traceId=%s method=%s path=%s client=%s", trace_id, request.method, request.url.path, request.client.host if request.client else "")
    response = await call_next(request)

    # 请求结束时输出状态码和耗时，辅助定位是否被鉴权、路由或业务逻辑拦截。
    duration_ms = elapsed_milliseconds(start_time)
    if trace_id:
        response.headers["X-Trace-Id"] = trace_id
    logger.info("Python AI 服务请求完成：traceId=%s method=%s path=%s status=%s durationMs=%s", trace_id, request.method, request.url.path, response.status_code, duration_ms)
    return response


@app.exception_handler(RequestValidationError)
async def log_validation_error(request: Request, exc: RequestValidationError) -> Response:
    """记录 FastAPI 422 参数校验失败详情，便于排查 Java 传参问题。"""
    # 只记录元信息，不打印鉴权头和请求体，避免答案内容或 Token 出现在日志中。
    logger.warning(
        "Python AI 服务请求参数校验失败：traceId=%s method=%s path=%s contentType=%s contentLength=%s errors=%s",
        request.headers.get("x-trace-id", ""),
        request.method,
        request.url.path,
        request.headers.get("content-type", ""),
        request.headers.get("content-length", ""),
        exc.errors(),
    )
    return await request_validation_exception_handler(request, exc)


@app.get("/health")
def health() -> dict[str, str]:
    """健康检查。"""
    return {"status": "UP"}


@app.get("/")
def root() -> dict[str, str]:
    """返回 AI 服务基础状态，避免浏览器访问根路径时出现 404。"""
    # 根路径只暴露基础状态和可访问入口，不返回内部鉴权信息。
    return {"service": "AI Learn Service", "status": "UP", "healthPath": "/health", "docsPath": "/docs"}


@app.get("/favicon.ico", include_in_schema=False)
def favicon() -> Response:
    """忽略浏览器自动请求的站点图标。"""
    # 当前服务不提供页面图标，返回 204 避免日志中出现无意义的 404。
    return Response(status_code=204)


# 刷题 Agent 为后端内部接口。
app.include_router(practice_router)
app.include_router(log_levels_router)
app.include_router(model_cache_router)
