package com.earth.online.player.ailearn.model.infrastructure;

import java.time.LocalDateTime;

/**
 * 用户模型权益数据库记录。
 */
public class UserModelEntitlementRecord {

    private Long id;
    private Long userId;
    private String modelLevel;
    private String entitlementKind;
    private String status;
    private Integer remainingDays;
    private LocalDateTime lastConsumedAt;
    private LocalDateTime startedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 获取权益ID。
     *
     * @return 权益ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置权益ID。
     *
     * @param id 权益ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户ID。
     *
     * @return 用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户ID。
     *
     * @param userId 用户ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取模型等级。
     *
     * @return 模型等级
     */
    public String getModelLevel() {
        return modelLevel;
    }

    /**
     * 设置模型等级。
     *
     * @param modelLevel 模型等级
     */
    public void setModelLevel(String modelLevel) {
        this.modelLevel = modelLevel;
    }

    /**
     * 获取权益类型。
     *
     * @return 权益类型
     */
    public String getEntitlementKind() {
        return entitlementKind;
    }

    /**
     * 设置权益类型。
     *
     * @param entitlementKind 权益类型
     */
    public void setEntitlementKind(String entitlementKind) {
        this.entitlementKind = entitlementKind;
    }

    /**
     * 获取权益状态。
     *
     * @return 权益状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置权益状态。
     *
     * @param status 权益状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取剩余天数。
     *
     * @return 剩余天数
     */
    public Integer getRemainingDays() {
        return remainingDays;
    }

    /**
     * 设置剩余天数。
     *
     * @param remainingDays 剩余天数
     */
    public void setRemainingDays(Integer remainingDays) {
        this.remainingDays = remainingDays;
    }

    /**
     * 获取最近扣减时间。
     *
     * @return 最近扣减时间
     */
    public LocalDateTime getLastConsumedAt() {
        return lastConsumedAt;
    }

    /**
     * 设置最近扣减时间。
     *
     * @param lastConsumedAt 最近扣减时间
     */
    public void setLastConsumedAt(LocalDateTime lastConsumedAt) {
        this.lastConsumedAt = lastConsumedAt;
    }

    /**
     * 获取首次生效时间。
     *
     * @return 首次生效时间
     */
    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    /**
     * 设置首次生效时间。
     *
     * @param startedAt 首次生效时间
     */
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     *
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
