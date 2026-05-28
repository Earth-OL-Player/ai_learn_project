from fastapi import APIRouter, Depends, HTTPException, status

from app.api.dependencies import verify_internal_token
from app.config.log_config import get_service_log_level, set_service_log_level
from app.schemas.common import ApiResponse
from app.schemas.log_levels import LogLevelRequest, LogLevelResponse

router = APIRouter(prefix="/internal/v1/log-levels", tags=["log-levels"])


@router.get("", response_model=ApiResponse[LogLevelResponse], dependencies=[Depends(verify_internal_token)])
def find_log_level() -> ApiResponse[LogLevelResponse]:
    """查询 AI 服务当前运行期日志级别。"""
    return ApiResponse(data=LogLevelResponse(level=get_service_log_level(), message="AI 服务日志级别可动态调整"))


@router.put("", response_model=ApiResponse[LogLevelResponse], dependencies=[Depends(verify_internal_token)])
def update_log_level(request: LogLevelRequest) -> ApiResponse[LogLevelResponse]:
    """更新 AI 服务当前运行期日志级别。"""
    try:
        level = set_service_log_level(request.level)
    except ValueError as exception:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(exception)) from exception

    # 返回更新后的级别，便于 Java 后端和管理页面回显。
    return ApiResponse(data=LogLevelResponse(level=level, message="AI 服务日志级别已生效"))
