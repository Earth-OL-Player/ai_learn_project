package com.earth.online.player.ailearn.rag.interfaces;

/**
 * RAG检索片段响应。
 *
 * @param sourceType 来源类型
 * @param sourceId 来源ID
 * @param title 标题
 * @param chunkText 片段文本
 * @param score 相似度
 */
public record RagSearchSnippetResponse(
        String sourceType,
        String sourceId,
        String title,
        String chunkText,
        double score
) {
}
