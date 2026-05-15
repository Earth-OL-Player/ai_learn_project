package com.earth.online.player.ailearn.rag.interfaces;

/**
 * RAG检索请求。
 *
 * @param query 检索文本
 * @param topK 返回数量
 */
public record RagSearchRequest(String query, Integer topK) {
}
