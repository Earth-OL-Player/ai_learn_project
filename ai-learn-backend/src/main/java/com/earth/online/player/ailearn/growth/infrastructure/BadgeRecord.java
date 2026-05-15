package com.earth.online.player.ailearn.growth.infrastructure;

import java.time.LocalDateTime;

/**
 * 徽章查询投影。
 */
public class BadgeRecord {

    private Long id;
    private String name;
    private String description;
    private String icon;
    private String ruleCode;
    private Boolean acquired;
    private LocalDateTime acquiredAt;

    /** 获取徽章ID。 */
    public Long getId() { return id; }

    /** 设置徽章ID。 */
    public void setId(Long id) { this.id = id; }

    /** 获取名称。 */
    public String getName() { return name; }

    /** 设置名称。 */
    public void setName(String name) { this.name = name; }

    /** 获取说明。 */
    public String getDescription() { return description; }

    /** 设置说明。 */
    public void setDescription(String description) { this.description = description; }

    /** 获取图标。 */
    public String getIcon() { return icon; }

    /** 设置图标。 */
    public void setIcon(String icon) { this.icon = icon; }

    /** 获取规则编码。 */
    public String getRuleCode() { return ruleCode; }

    /** 设置规则编码。 */
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    /** 获取是否已获得。 */
    public Boolean getAcquired() { return acquired; }

    /** 设置是否已获得。 */
    public void setAcquired(Boolean acquired) { this.acquired = acquired; }

    /** 获取获得时间。 */
    public LocalDateTime getAcquiredAt() { return acquiredAt; }

    /** 设置获得时间。 */
    public void setAcquiredAt(LocalDateTime acquiredAt) { this.acquiredAt = acquiredAt; }
}
