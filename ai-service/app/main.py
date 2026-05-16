from fastapi import FastAPI

from app.api.practice import router as practice_router
from app.api.rag import router as rag_router

app = FastAPI(title="AI Learn Service", version="0.2.0")


@app.get("/health")
def health() -> dict[str, str]:
    """健康检查。"""
    return {"status": "UP"}


# RAG 和刷题 Agent 均为后端内部接口。
app.include_router(rag_router)
app.include_router(practice_router)
