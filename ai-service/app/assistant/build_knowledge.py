"""从受限的公开 Markdown 构建知识索引：python -m app.assistant.build_knowledge。"""

from __future__ import annotations

import json
import re
from pathlib import Path


def main() -> None:
    root = Path(__file__).resolve().parents[3]
    output = Path(__file__).with_name("knowledge") / "public-index.json"
    sources = [(root / "README.md", "/home")]
    sources.extend((path, "/learning-roadmap") for path in sorted((root / "ai-learn-web/src/content/learning-roadmap").glob("*.md")))
    documents = []
    for path, route in sources:
        text = path.read_text(encoding="utf-8")
        text = re.sub(r"!\[[^\]]*\]\([^)]*\)", "", text)
        for ordinal, block in enumerate(re.split(r"(?m)(?=^#{1,4} )", text)):
            block = block.strip()
            if not block:
                continue
            title = block.splitlines()[0].lstrip("# ")[:120]
            for part, start in enumerate(range(0, len(block), 1200)):
                documents.append({"id": f"public-{path.stem}-{ordinal}-{part}", "title": title, "path": route, "content": block[start:start + 1200]})
    output.write_text(json.dumps(documents, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"公开知识索引已生成：{len(documents)} 个片段")


if __name__ == "__main__":
    main()
