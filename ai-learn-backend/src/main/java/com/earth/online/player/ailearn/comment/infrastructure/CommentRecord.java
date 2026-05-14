package com.earth.online.player.ailearn.comment.infrastructure;

import java.time.LocalDateTime;

/**
 * 评论列表查询投影。
 */
public class CommentRecord {

    private Long id;
    private String content;
    private Long parentId;
    private Integer likeCount;
    private Long authorId;
    private String authorUsername;
    private String authorNickname;
    private String authorAvatar;
    private LocalDateTime createdAt;

    /** 获取评论ID。 */
    public Long getId() { return id; }

    /** 设置评论ID。 */
    public void setId(Long id) { this.id = id; }

    /** 获取内容。 */
    public String getContent() { return content; }

    /** 设置内容。 */
    public void setContent(String content) { this.content = content; }

    /** 获取父评论ID。 */
    public Long getParentId() { return parentId; }

    /** 设置父评论ID。 */
    public void setParentId(Long parentId) { this.parentId = parentId; }

    /** 获取点赞数。 */
    public Integer getLikeCount() { return likeCount; }

    /** 设置点赞数。 */
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    /** 获取作者ID。 */
    public Long getAuthorId() { return authorId; }

    /** 设置作者ID。 */
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    /** 获取作者用户名。 */
    public String getAuthorUsername() { return authorUsername; }

    /** 设置作者用户名。 */
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    /** 获取作者昵称。 */
    public String getAuthorNickname() { return authorNickname; }

    /** 设置作者昵称。 */
    public void setAuthorNickname(String authorNickname) { this.authorNickname = authorNickname; }

    /** 获取作者头像。 */
    public String getAuthorAvatar() { return authorAvatar; }

    /** 设置作者头像。 */
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }

    /** 获取创建时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** 设置创建时间。 */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
