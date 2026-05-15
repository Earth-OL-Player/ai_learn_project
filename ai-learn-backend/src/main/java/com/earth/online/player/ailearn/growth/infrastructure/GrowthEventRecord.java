package com.earth.online.player.ailearn.growth.infrastructure;

import java.time.LocalDateTime;

/**
 * 成长事件查询投影。
 */
public class GrowthEventRecord {

    private Long id;
    private String eventType;
    private String title;
    private String description;
    private Integer experienceDelta;
    private LocalDateTime createdAt;

    /** 获取事件ID。 */
    public Long getId() { return id; }

    /** 设置事件ID。 */
    public void setId(Long id) { this.id = id; }

    /** 获取事件类型。 */
    public String getEventType() { return eventType; }

    /** 设置事件类型。 */
    public void setEventType(String eventType) { this.eventType = eventType; }

    /** 获取标题。 */
    public String getTitle() { return title; }

    /** 设置标题。 */
    public void setTitle(String title) { this.title = title; }

    /** 获取说明。 */
    public String getDescription() { return description; }

    /** 设置说明。 */
    public void setDescription(String description) { this.description = description; }

    /** 获取经验变化。 */
    public Integer getExperienceDelta() { return experienceDelta; }

    /** 设置经验变化。 */
    public void setExperienceDelta(Integer experienceDelta) { this.experienceDelta = experienceDelta; }

    /** 获取创建时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** 设置创建时间。 */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
