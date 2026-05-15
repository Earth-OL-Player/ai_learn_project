from __future__ import annotations

from qdrant_client import QdrantClient
from qdrant_client.http.models import Distance, PointStruct, VectorParams

from app.config.settings import settings
from app.rag.embedding import embed_text
from app.schemas.rag import RagSearchSnippet


class QdrantRepository:
    """Qdrant 向量仓储。"""

    def __init__(self) -> None:
        self.client = QdrantClient(url=settings.qdrant_url)
        self.collection = settings.qdrant_collection

    def ensure_collection(self) -> None:
        """创建或复用集合。"""
        collections = self.client.get_collections().collections
        if any(item.name == self.collection for item in collections):
            return
        self.client.create_collection(
            collection_name=self.collection,
            vectors_config=VectorParams(size=64, distance=Distance.COSINE),
        )

    def upsert_chunks(self, chunks: list[dict]) -> None:
        """写入向量片段。"""
        self.ensure_collection()
        points = []
        for item in chunks:
            chunk_text = item["chunkText"]
            point_id = abs(hash(f"{item['sourceType']}:{item['sourceId']}:{chunk_text}")) % (2**63)
            points.append(PointStruct(id=point_id, vector=embed_text(chunk_text), payload=item))
        if points:
            self.client.upsert(collection_name=self.collection, points=points)

    def search(self, query: str, top_k: int) -> list[RagSearchSnippet]:
        """检索向量片段。"""
        self.ensure_collection()
        results = self.client.search(collection_name=self.collection, query_vector=embed_text(query), limit=top_k)
        snippets: list[RagSearchSnippet] = []
        for result in results:
            payload = result.payload or {}
            snippets.append(
                RagSearchSnippet(
                    sourceType=str(payload.get("sourceType", "UNKNOWN")),
                    sourceId=str(payload.get("sourceId", "")),
                    title=str(payload.get("title", "")),
                    chunkText=str(payload.get("chunkText", "")),
                    score=float(result.score),
                )
            )
        return snippets
