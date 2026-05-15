from __future__ import annotations

import uuid

from app.rag.chunker import chunk_text
from app.rag.qdrant_repository import QdrantRepository
from app.schemas.rag import RagIndexTaskRequest, RagIndexTaskResponse, RagSearchSnippet

_TASKS: dict[str, RagIndexTaskResponse] = {}


class RagService:
    """RAG 入库和检索服务。"""

    def __init__(self) -> None:
        self.repository = QdrantRepository()

    def submit_index_task(self, request: RagIndexTaskRequest) -> RagIndexTaskResponse:
        """同步完成最小入库任务。"""
        task_id = str(uuid.uuid4())
        response = RagIndexTaskResponse(taskId=task_id, status="RUNNING", message="任务执行中")
        _TASKS[task_id] = response
        try:
            chunks: list[dict] = []
            for document in request.documents:
                text = str(document.get("content", ""))
                for chunk in chunk_text(text):
                    chunks.append(
                        {
                            "sourceType": request.sourceType,
                            "sourceId": str(document.get("sourceId", "")),
                            "title": str(document.get("title", "")),
                            "knowledgePoints": document.get("knowledgePoints", []),
                            "difficulty": document.get("difficulty", ""),
                            "chunkText": chunk,
                        }
                    )
            self.repository.upsert_chunks(chunks)
            response = RagIndexTaskResponse(taskId=task_id, status="SUCCESS", message=f"已入库 {len(chunks)} 个片段")
        except Exception as exc:  # noqa: BLE001 - 需要把第三方异常转换为任务失败状态
            response = RagIndexTaskResponse(taskId=task_id, status="FAILED", message=f"入库失败：{exc.__class__.__name__}")
        _TASKS[task_id] = response
        return response

    def get_task(self, task_id: str) -> RagIndexTaskResponse:
        """查询任务状态。"""
        return _TASKS.get(task_id, RagIndexTaskResponse(taskId=task_id, status="FAILED", message="任务不存在"))

    def search(self, query: str, top_k: int) -> list[RagSearchSnippet]:
        """检索知识片段。"""
        return self.repository.search(query, max(1, min(top_k, 20)))


rag_service = RagService()
