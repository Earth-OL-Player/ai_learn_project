from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse

from app.api.dependencies import verify_internal_token
from app.assistant.agent import AssistantStepRequest, stream_step

router = APIRouter(prefix="/internal/v1/assistant", tags=["assistant"], dependencies=[Depends(verify_internal_token)])


@router.post("/steps/stream")
def assistant_step(request: AssistantStepRequest) -> StreamingResponse:
    """内部助手分段执行接口，浏览器不得直连。"""
    return StreamingResponse(stream_step(request), media_type="text/event-stream", headers={"Cache-Control": "no-cache"})
