from fastapi import FastAPI

from app.api.grading import router as grading_router
from app.api.rag import router as rag_router

app = FastAPI(title="AI Learn Service", version="0.1.0")


@app.get("/health")
def health() -> dict[str, str]:
    """健康检查。"""
    return {"status": "UP"}


app.include_router(grading_router)
app.include_router(rag_router)
