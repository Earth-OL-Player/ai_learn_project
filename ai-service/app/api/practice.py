from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse

from app.api.dependencies import verify_internal_token
from app.practice.agent_service import practice_agent_service
from app.schemas.common import ApiResponse
from app.schemas.practice import (
    PracticeDiscussRequest,
    PracticeDiscussResponse,
    PracticeGradeRequest,
    PracticeGradeResponse,
    PracticeRelevanceRequest,
    PracticeRelevanceResponse,
)

router = APIRouter(prefix="/internal/v1/practice", tags=["practice"])


@router.post("/answer/grade", response_model=ApiResponse[PracticeGradeResponse], dependencies=[Depends(verify_internal_token)])
def grade_answer(request: PracticeGradeRequest) -> ApiResponse[PracticeGradeResponse]:
    """对用户答案进行结构化评分。"""
    return ApiResponse(data=practice_agent_service.grade_answer(request))


@router.post("/discuss", response_model=ApiResponse[PracticeDiscussResponse], dependencies=[Depends(verify_internal_token)])
def discuss(request: PracticeDiscussRequest) -> ApiResponse[PracticeDiscussResponse]:
    """围绕当前题继续讨论学习。"""
    return ApiResponse(data=practice_agent_service.discuss(request))


@router.post("/discuss/stream", dependencies=[Depends(verify_internal_token)])
def discuss_stream(request: PracticeDiscussRequest) -> StreamingResponse:
    """围绕当前题继续流式讨论学习。"""
    return StreamingResponse(practice_agent_service.stream_discuss(request), media_type="text/event-stream")


@router.post("/relevance", response_model=ApiResponse[PracticeRelevanceResponse], dependencies=[Depends(verify_internal_token)])
def judge_relevance(request: PracticeRelevanceRequest) -> ApiResponse[PracticeRelevanceResponse]:
    """判断用户输入是否与当前刷题上下文相关。"""
    return ApiResponse(data=practice_agent_service.judge_relevance(request))
