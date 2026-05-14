package com.earth.online.player.ailearn.suggestion.domain;

import java.time.LocalDateTime;

/**
 * 建议领域对象。
 */
public class Suggestion {

    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String type;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;

    /** 获取建议ID。 */
    public Long getId() {
        return id;
    }

    /** 设置建议ID。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 获取提交用户ID。 */
    public Long getUserId() {
        return userId;
    }

    /** 设置提交用户ID。 */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** 获取建议标题。 */
    public String getTitle() {
        return title;
    }

    /** 设置建议标题。 */
    public void setTitle(String title) {
        this.title = title;
    }

    /** 获取建议内容。 */
    public String getContent() {
        return content;
    }

    /** 设置建议内容。 */
    public void setContent(String content) {
        this.content = content;
    }

    /** 获取建议类型。 */
    public String getType() {
        return type;
    }

    /** 设置建议类型。 */
    public void setType(String type) {
        this.type = type;
    }

    /** 获取处理状态。 */
    public String getStatus() {
        return status;
    }

    /** 设置处理状态。 */
    public void setStatus(String status) {
        this.status = status;
    }

    /** 获取创建时间。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 设置创建时间。 */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** 获取更新时间。 */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** 设置更新时间。 */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** 获取逻辑删除标识。 */
    public Boolean getDeleted() {
        return deleted;
    }

    /** 设置逻辑删除标识。 */
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
