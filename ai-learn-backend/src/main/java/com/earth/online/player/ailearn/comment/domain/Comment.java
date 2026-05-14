package com.earth.online.player.ailearn.comment.domain;

import java.time.LocalDateTime;

/**
 * 评论领域对象。
 */
public class Comment {

    private Long id;
    private Long userId;
    private String content;
    private Long parentId;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;

    /** 获取评论ID。 */
    public Long getId() { return id; }

    /** 设置评论ID。 */
    public void setId(Long id) { this.id = id; }

    /** 获取评论用户ID。 */
    public Long getUserId() { return userId; }

    /** 设置评论用户ID。 */
    public void setUserId(Long userId) { this.userId = userId; }

    /** 获取评论内容。 */
    public String getContent() { return content; }

    /** 设置评论内容。 */
    public void setContent(String content) { this.content = content; }

    /** 获取父评论ID。 */
    public Long getParentId() { return parentId; }

    /** 设置父评论ID。 */
    public void setParentId(Long parentId) { this.parentId = parentId; }

    /** 获取点赞数。 */
    public Integer getLikeCount() { return likeCount; }

    /** 设置点赞数。 */
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    /** 获取创建时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** 设置创建时间。 */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** 获取更新时间。 */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** 设置更新时间。 */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /** 获取逻辑删除标识。 */
    public Boolean getDeleted() { return deleted; }

    /** 设置逻辑删除标识。 */
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
