def chunk_text(text: str, size: int = 500) -> list[str]:
    """按固定长度切分文本。"""
    cleaned = text.strip()
    if not cleaned:
        return []
    return [cleaned[index:index + size] for index in range(0, len(cleaned), size)]
