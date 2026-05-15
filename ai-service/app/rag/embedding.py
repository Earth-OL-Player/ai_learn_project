from __future__ import annotations

import hashlib


def embed_text(text: str, dimension: int = 64) -> list[float]:
    """生成确定性的本地占位向量，便于无模型 Key 时完成本地联调。"""
    digest = hashlib.sha256(text.encode("utf-8")).digest()
    values: list[float] = []
    while len(values) < dimension:
        for byte in digest:
            values.append((byte / 255.0) * 2 - 1)
            if len(values) >= dimension:
                break
        digest = hashlib.sha256(digest).digest()
    return values
