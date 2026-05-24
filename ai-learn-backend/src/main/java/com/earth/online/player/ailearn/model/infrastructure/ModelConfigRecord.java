package com.earth.online.player.ailearn.model.infrastructure;

import java.time.LocalDateTime;

/**
 * 模型配置数据库记录。
 */
public class ModelConfigRecord {

    private Long id;
    private String modelLevel;
    private String modelName;
    private String baseUrl;
    private String apiKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 获取记录ID。
     *
     * @return 记录ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置记录ID。
     *
     * @param id 记录ID
     */
    public void setId(Long id) {
        this.id = id;
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
     * 获取模型名称。
     *
     * @return 模型名称
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * 设置模型名称。
     *
     * @param modelName 模型名称
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * 获取模型基础地址。
     *
     * @return 模型基础地址
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 设置模型基础地址。
     *
     * @param baseUrl 模型基础地址
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * 获取模型 API Key。
     *
     * @return 模型 API Key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * 设置模型 API Key。
     *
     * @param apiKey 模型 API Key
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
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
