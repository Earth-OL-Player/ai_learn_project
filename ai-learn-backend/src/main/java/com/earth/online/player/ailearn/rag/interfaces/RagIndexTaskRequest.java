package com.earth.online.player.ailearn.rag.interfaces;

/**
 * RAG入库任务提交请求。
 *
 * @param sourceType 来源类型
 */
public record RagIndexTaskRequest(String sourceType) {
}
