package com.earth.online.player.ailearn.suggestion.infrastructure;

import java.time.LocalDateTime;

/**
 * 建议列表查询投影。
 */
public class SuggestionRecord {

    private Long id;
    private String title;
    private String content;
    private String type;
    private String status;
    private Long authorId;
    private String authorUsername;
    private String authorNickname;
    private String authorAvatar;
    private LocalDateTime createdAt;

    /** 获取建议ID。 */
    public Long getId() { return id; }

    /** 设置建议ID。 */
    public void setId(Long id) { this.id = id; }

    /** 获取标题。 */
    public String getTitle() { return title; }

    /** 设置标题。 */
    public void setTitle(String title) { this.title = title; }

    /** 获取内容。 */
    public String getContent() { return content; }

    /** 设置内容。 */
    public void setContent(String content) { this.content = content; }

    /** 获取类型。 */
    public String getType() { return type; }

    /** 设置类型。 */
    public void setType(String type) { this.type = type; }

    /** 获取状态。 */
    public String getStatus() { return status; }

    /** 设置状态。 */
    public void setStatus(String status) { this.status = status; }

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
