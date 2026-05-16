package com.earth.online.player.ailearn.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 服务调用配置。
 */
@ConfigurationProperties(prefix = "app.ai-service")
public class AiServiceProperties {

    private boolean enabled;
    private String baseUrl = "http://127.0.0.1:8000";
    private String token = "AI_SERVICE_TOKEN占位符";
    private int timeoutSeconds = 15;

    /**
     * 判断是否启用 AI 服务调用。
     *
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用 AI 服务调用。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取 AI 服务基础地址。
     *
     * @return 服务地址
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 设置 AI 服务基础地址。
     *
     * @param baseUrl 服务地址
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * 获取内部鉴权 Token。
     *
     * @return 鉴权 Token
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置内部鉴权 Token。
     *
     * @param token 鉴权 Token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 获取调用超时时间。
     *
     * @return 超时秒数
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * 设置调用超时时间。
     *
     * @param timeoutSeconds 超时秒数
     */
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
