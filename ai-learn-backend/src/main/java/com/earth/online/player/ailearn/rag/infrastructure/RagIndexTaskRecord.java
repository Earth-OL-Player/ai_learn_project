package com.earth.online.player.ailearn.rag.infrastructure;

import java.time.LocalDateTime;

/**
 * RAG入库任务查询投影。
 */
public class RagIndexTaskRecord {

    private String taskId;
    private String sourceType;
    private String status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 获取任务ID。 */
    public String getTaskId() { return taskId; }

    /** 设置任务ID。 */
    public void setTaskId(String taskId) { this.taskId = taskId; }

    /** 获取来源类型。 */
    public String getSourceType() { return sourceType; }

    /** 设置来源类型。 */
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    /** 获取任务状态。 */
    public String getStatus() { return status; }

    /** 设置任务状态。 */
    public void setStatus(String status) { this.status = status; }

    /** 获取任务消息。 */
    public String getMessage() { return message; }

    /** 设置任务消息。 */
    public void setMessage(String message) { this.message = message; }

    /** 获取创建时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** 设置创建时间。 */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** 获取更新时间。 */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** 设置更新时间。 */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
