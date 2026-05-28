from fastapi import APIRouter, Depends

from app.api.dependencies import verify_internal_token
from app.practice.agent_service import practice_agent_service
from app.schemas.common import ApiResponse
from app.schemas.model_cache import ModelCacheInvalidationResponse

router = APIRouter(prefix="/internal/v1/model-cache", tags=["model-cache"])


@router.post(
    "/invalidate",
    response_model=ApiResponse[ModelCacheInvalidationResponse],
    dependencies=[Depends(verify_internal_token)],
)
def invalidate_model_cache() -> ApiResponse[ModelCacheInvalidationResponse]:
    """清理模型和 Agent 构造缓存。"""
    cleared_count = practice_agent_service.clear_cached_model_objects()

    # 只返回清理数量，不暴露模型名称、供应商地址或密钥摘要。
    return ApiResponse(data=ModelCacheInvalidationResponse(clearedObjects=cleared_count, message="模型缓存已清理"))
