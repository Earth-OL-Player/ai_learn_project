from fastapi import APIRouter, Depends

from app.api.dependencies import verify_internal_token
from app.schemas.grading import ApiResponse
from app.schemas.rag import RagIndexTaskRequest, RagIndexTaskResponse, RagSearchRequest, RagSearchSnippet
from app.rag.index_task_service import rag_service

router = APIRouter(prefix="/internal/v1/rag", tags=["rag"])


@router.post("/index-tasks", response_model=ApiResponse[RagIndexTaskResponse], dependencies=[Depends(verify_internal_token)])
def submit_index_task(request: RagIndexTaskRequest) -> ApiResponse[RagIndexTaskResponse]:
    """提交 RAG 入库任务。"""
    return ApiResponse(data=rag_service.submit_index_task(request))


@router.get("/index-tasks/{task_id}", response_model=ApiResponse[RagIndexTaskResponse], dependencies=[Depends(verify_internal_token)])
def get_index_task(task_id: str) -> ApiResponse[RagIndexTaskResponse]:
    """查询 RAG 入库任务。"""
    return ApiResponse(data=rag_service.get_task(task_id))


@router.post("/search", response_model=ApiResponse[list[RagSearchSnippet]], dependencies=[Depends(verify_internal_token)])
def search(request: RagSearchRequest) -> ApiResponse[list[RagSearchSnippet]]:
    """检索知识片段。"""
    return ApiResponse(data=rag_service.search(request.query, request.topK))
