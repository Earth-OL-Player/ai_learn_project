from fastapi import APIRouter, Depends, HTTPException, status
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
def grade_answer(request: PracticeGradeRequest) -> ApiResponse[PracticeGradeResponse]:
    """对用户答案进行结构化评分。"""
    grading = practice_agent_service.grade_answer(request)
    if grading is None:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="大模型评分暂不可用")
    return ApiResponse(data=grading)


@router.post("/discuss/stream", dependencies=[Depends(verify_internal_token)])
def discuss_stream(request: PracticeDiscussRequest) -> StreamingResponse:
    """围绕当前题继续流式讨论学习。"""
    return StreamingResponse(practice_agent_service.stream_discuss(request), media_type="text/event-stream")
