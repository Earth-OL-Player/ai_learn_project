package com.earth.online.player.ailearn.model.infrastructure;

import java.time.LocalDateTime;

/**
 * 兑换码数据库记录。
 */
public class RedemptionCodeRecord {

    private Long id;
    private String code;
    private String codeType;
    private String status;
    private Long usedByUserId;
    private String usedByUsername;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;

    /**
     * 获取兑换码ID。
     *
     * @return 兑换码ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置兑换码ID。
     *
     * @param id 兑换码ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取兑换码。
     *
     * @return 兑换码
     */
    public String getCode() {
        return code;
    }

    /**
     * 设置兑换码。
     *
     * @param code 兑换码
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 获取兑换码类型。
     *
     * @return 兑换码类型
     */
    public String getCodeType() {
        return codeType;
    }

    /**
     * 设置兑换码类型。
     *
     * @param codeType 兑换码类型
     */
    public void setCodeType(String codeType) {
        this.codeType = codeType;
    }

    /**
     * 获取兑换状态。
     *
     * @return 兑换状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置兑换状态。
     *
     * @param status 兑换状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取兑换用户ID。
     *
     * @return 兑换用户ID
     */
    public Long getUsedByUserId() {
        return usedByUserId;
    }

    /**
     * 设置兑换用户ID。
     *
     * @param usedByUserId 兑换用户ID
     */
    public void setUsedByUserId(Long usedByUserId) {
        this.usedByUserId = usedByUserId;
    }

    /**
     * 获取兑换用户名。
     *
     * @return 兑换用户名
     */
    public String getUsedByUsername() {
        return usedByUsername;
    }

    /**
     * 设置兑换用户名。
     *
     * @param usedByUsername 兑换用户名
     */
    public void setUsedByUsername(String usedByUsername) {
        this.usedByUsername = usedByUsername;
    }

    /**
     * 获取兑换时间。
     *
     * @return 兑换时间
     */
    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    /**
     * 设置兑换时间。
     *
     * @param usedAt 兑换时间
     */
    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
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

    /**
     * 获取逻辑删除标识。
     *
     * @return 是否删除
     */
    public Boolean getDeleted() {
        return deleted;
    }

    /**
     * 设置逻辑删除标识。
     *
     * @param deleted 是否删除
     */
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
