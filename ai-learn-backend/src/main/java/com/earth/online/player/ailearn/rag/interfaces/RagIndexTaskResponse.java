package com.earth.online.player.ailearn.rag.interfaces;

import java.time.OffsetDateTime;

/**
 * RAG入库任务响应。
 *
 * @param taskId 任务ID
 * @param sourceType 来源类型
 * @param status 状态
 * @param message 消息
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record RagIndexTaskResponse(
        String taskId,
        String sourceType,
        String status,
        String message,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
