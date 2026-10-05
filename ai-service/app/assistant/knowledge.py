from __future__ import annotations

import re
import json
from functools import lru_cache
from pathlib import Path

KNOWLEDGE_DIR = Path(__file__).with_name("knowledge")


@lru_cache(maxsize=1)
def documents() -> list[dict[str, str]]:
    """只读取随服务部署的公开知识，不扫描私有配置或整个仓库。"""
    result = []
    for path in sorted(KNOWLEDGE_DIR.glob("*.md")):
        text = path.read_text(encoding="utf-8")
        title, route, body = text.split("\n", 2)
        result.append({"id": path.stem, "title": title.removeprefix("# "), "path": route.strip(), "content": body.strip()})
    index = KNOWLEDGE_DIR / "public-index.json"
    if index.exists():
        result.extend(json.loads(index.read_text(encoding="utf-8")))
    return result


def search(query: str) -> list[dict[str, str]]:
    terms = re.findall(r"[a-zA-Z0-9_]+", query.lower())
    for phrase in re.findall(r"[\u4e00-\u9fff]+", query):
        terms.extend(phrase[index:index + 2] for index in range(len(phrase) - 1))
    ranked = sorted(documents(), key=lambda doc: sum(3 * (term in doc["title"].lower()) + (term in doc["content"].lower()) for term in set(terms)), reverse=True)
    return ranked[:3]
