from fastapi import APIRouter, Depends

from app.api.dependencies import verify_internal_token
from app.practice.agent_service import practice_agent_service
from app.schemas.common import ApiResponse
from app.schemas.practice import PracticeDiscussRequest, PracticeDiscussResponse, PracticeGradeRequest, PracticeGradeResponse

router = APIRouter(prefix="/internal/v1/practice", tags=["practice"])


@router.post("/answer/grade", response_model=ApiResponse[PracticeGradeResponse], dependencies=[Depends(verify_internal_token)])
def grade_answer(request: PracticeGradeRequest) -> ApiResponse[PracticeGradeResponse]:
    """对用户答案进行结构化评分。"""
    return ApiResponse(data=practice_agent_service.grade_answer(request))


@router.post("/discuss", response_model=ApiResponse[PracticeDiscussResponse], dependencies=[Depends(verify_internal_token)])
def discuss(request: PracticeDiscussRequest) -> ApiResponse[PracticeDiscussResponse]:
    """围绕当前题继续讨论学习。"""
    return ApiResponse(data=practice_agent_service.discuss(request))
