import uuid

from fastapi import APIRouter, Depends, Header, HTTPException, status
from fastapi.responses import StreamingResponse

from app.api.dependencies import verify_internal_token
from app.practice.agent_service import practice_agent_service
from app.schemas.common import ApiResponse
from app.schemas.practice import (
    PracticeDiscussRequest,
    PracticeGradeRequest,
    PracticeGradeResponse,
)

router = APIRouter(prefix="/internal/v1/practice", tags=["practice"])


@router.post("/answer/grade", response_model=ApiResponse[PracticeGradeResponse], dependencies=[Depends(verify_internal_token)])
def grade_answer(request: PracticeGradeRequest, x_trace_id: str | None = Header(default=None)) -> ApiResponse[PracticeGradeResponse]:
    """对用户答案进行结构化评分。"""
    trace_id = _resolve_trace_id(x_trace_id)
    result = practice_agent_service.grade_answer(request, trace_id)
    if result.grading is None:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="大模型评分暂不可用")
    return ApiResponse(data=result.grading, traceId=trace_id, observability=result.metrics.model_dump(mode="json"))


@router.post("/discuss/stream", dependencies=[Depends(verify_internal_token)])
def discuss_stream(request: PracticeDiscussRequest, x_trace_id: str | None = Header(default=None)) -> StreamingResponse:
    """围绕当前题继续流式讨论学习。"""
    trace_id = _resolve_trace_id(x_trace_id)
    return StreamingResponse(practice_agent_service.stream_discuss(request, trace_id), media_type="text/event-stream", headers={"X-Trace-Id": trace_id})


def _resolve_trace_id(trace_id: str | None) -> str:
    """解析 Java 透传的 traceId，缺失时生成兜底值。"""
    if trace_id and trace_id.strip():
        return trace_id.strip()
    return uuid.uuid4().hex
