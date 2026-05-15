package com.earth.online.player.ailearn.agent.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 服务调用配置。
 */
@ConfigurationProperties(prefix = "app.ai-service")
public class AiServiceProperties {

    private boolean enabled;
    private String baseUrl;
    private String token;
    private int timeoutSeconds = 5;

    /** 获取是否启用。 */
    public boolean isEnabled() { return enabled; }

    /** 设置是否启用。 */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** 获取服务地址。 */
    public String getBaseUrl() { return baseUrl; }

    /** 设置服务地址。 */
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    /** 获取内部令牌。 */
    public String getToken() { return token; }

    /** 设置内部令牌。 */
    public void setToken(String token) { this.token = token; }

    /** 获取超时时间。 */
    public int getTimeoutSeconds() { return timeoutSeconds; }

    /** 设置超时时间。 */
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
