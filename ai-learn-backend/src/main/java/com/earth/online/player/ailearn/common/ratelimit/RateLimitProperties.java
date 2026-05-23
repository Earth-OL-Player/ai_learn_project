package com.earth.online.player.ailearn.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 内存限流配置。
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private int loginLimit = 10;
    private int loginWindowSeconds = 60;
    private int registerLimit = 3;
    private int registerWindowSeconds = 3600;
    private int likeLimit = 30;
    private int likeWindowSeconds = 60;
    private int commentLimit = 10;
    private int commentWindowSeconds = 60;
    private int csvImportLimit = 3;
    private int csvImportWindowSeconds = 600;
    private int aiRequestLimit = 8;
    private int aiRequestWindowSeconds = 60;
    private int aiConcurrentLimit = 1;

    /**
     * 判断是否启用限流。
     *
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用限流。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取登录窗口内允许次数。
     *
     * @return 登录限流次数
     */
    public int getLoginLimit() {
        return loginLimit;
    }

    /**
     * 设置登录窗口内允许次数。
     *
     * @param loginLimit 登录限流次数
     */
    public void setLoginLimit(int loginLimit) {
        this.loginLimit = loginLimit;
    }

    /**
     * 获取登录限流窗口秒数。
     *
     * @return 窗口秒数
     */
    public int getLoginWindowSeconds() {
        return loginWindowSeconds;
    }

    /**
     * 设置登录限流窗口秒数。
     *
     * @param loginWindowSeconds 窗口秒数
     */
    public void setLoginWindowSeconds(int loginWindowSeconds) {
        this.loginWindowSeconds = loginWindowSeconds;
    }

    /**
     * 获取注册窗口内允许次数。
     *
     * @return 注册限流次数
     */
    public int getRegisterLimit() {
        return registerLimit;
    }

    /**
     * 设置注册窗口内允许次数。
     *
     * @param registerLimit 注册限流次数
     */
    public void setRegisterLimit(int registerLimit) {
        this.registerLimit = registerLimit;
    }

    /**
     * 获取注册限流窗口秒数。
     *
     * @return 窗口秒数
     */
    public int getRegisterWindowSeconds() {
        return registerWindowSeconds;
    }

    /**
     * 设置注册限流窗口秒数。
     *
     * @param registerWindowSeconds 窗口秒数
     */
    public void setRegisterWindowSeconds(int registerWindowSeconds) {
        this.registerWindowSeconds = registerWindowSeconds;
    }

    /**
     * 获取点赞窗口内允许次数。
     *
     * @return 点赞限流次数
     */
    public int getLikeLimit() {
        return likeLimit;
    }

    /**
     * 设置点赞窗口内允许次数。
     *
     * @param likeLimit 点赞限流次数
     */
    public void setLikeLimit(int likeLimit) {
        this.likeLimit = likeLimit;
    }

    /**
     * 获取点赞限流窗口秒数。
     *
     * @return 窗口秒数
     */
    public int getLikeWindowSeconds() {
        return likeWindowSeconds;
    }

    /**
     * 设置点赞限流窗口秒数。
     *
     * @param likeWindowSeconds 窗口秒数
     */
    public void setLikeWindowSeconds(int likeWindowSeconds) {
        this.likeWindowSeconds = likeWindowSeconds;
    }

    /**
     * 获取评论窗口内允许次数。
     *
     * @return 评论限流次数
     */
    public int getCommentLimit() {
        return commentLimit;
    }

    /**
     * 设置评论窗口内允许次数。
     *
     * @param commentLimit 评论限流次数
     */
    public void setCommentLimit(int commentLimit) {
        this.commentLimit = commentLimit;
    }

    /**
     * 获取评论限流窗口秒数。
     *
     * @return 窗口秒数
     */
    public int getCommentWindowSeconds() {
        return commentWindowSeconds;
    }

    /**
     * 设置评论限流窗口秒数。
     *
     * @param commentWindowSeconds 窗口秒数
     */
    public void setCommentWindowSeconds(int commentWindowSeconds) {
        this.commentWindowSeconds = commentWindowSeconds;
    }

    /**
     * 获取 CSV 导入窗口内允许次数。
     *
     * @return CSV 导入限流次数
     */
    public int getCsvImportLimit() {
        return csvImportLimit;
    }

    /**
     * 设置 CSV 导入窗口内允许次数。
     *
     * @param csvImportLimit CSV 导入限流次数
     */
    public void setCsvImportLimit(int csvImportLimit) {
        this.csvImportLimit = csvImportLimit;
    }

    /**
     * 获取 CSV 导入限流窗口秒数。
     *
     * @return 窗口秒数
     */
    public int getCsvImportWindowSeconds() {
        return csvImportWindowSeconds;
    }

    /**
     * 设置 CSV 导入限流窗口秒数。
     *
     * @param csvImportWindowSeconds 窗口秒数
     */
    public void setCsvImportWindowSeconds(int csvImportWindowSeconds) {
        this.csvImportWindowSeconds = csvImportWindowSeconds;
    }

    /**
     * 获取 AI 请求窗口内允许次数。
     *
     * @return AI 请求限流次数
     */
    public int getAiRequestLimit() {
        return aiRequestLimit;
    }

    /**
     * 设置 AI 请求窗口内允许次数。
     *
     * @param aiRequestLimit AI 请求限流次数
     */
    public void setAiRequestLimit(int aiRequestLimit) {
        this.aiRequestLimit = aiRequestLimit;
    }

    /**
     * 获取 AI 请求限流窗口秒数。
     *
     * @return 窗口秒数
     */
    public int getAiRequestWindowSeconds() {
        return aiRequestWindowSeconds;
    }

    /**
     * 设置 AI 请求限流窗口秒数。
     *
     * @param aiRequestWindowSeconds 窗口秒数
     */
    public void setAiRequestWindowSeconds(int aiRequestWindowSeconds) {
        this.aiRequestWindowSeconds = aiRequestWindowSeconds;
    }

    /**
     * 获取单用户 AI 并发上限。
     *
     * @return 并发上限
     */
    public int getAiConcurrentLimit() {
        return aiConcurrentLimit;
    }

    /**
     * 设置单用户 AI 并发上限。
     *
     * @param aiConcurrentLimit 并发上限
     */
    public void setAiConcurrentLimit(int aiConcurrentLimit) {
        this.aiConcurrentLimit = aiConcurrentLimit;
    }
}
