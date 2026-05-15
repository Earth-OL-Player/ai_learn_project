from fastapi import APIRouter, Depends

from app.api.dependencies import verify_internal_token
from app.answer_grading.service import grade_answer
from app.schemas.grading import ApiResponse, GradeAnswerRequest, GradeAnswerResponse

router = APIRouter(prefix="/internal/v1/agent", tags=["agent"])


@router.post("/answer/grade", response_model=ApiResponse[GradeAnswerResponse], dependencies=[Depends(verify_internal_token)])
def grade(request: GradeAnswerRequest) -> ApiResponse[GradeAnswerResponse]:
    """内部答案评分接口。"""
    return ApiResponse(data=grade_answer(request))
