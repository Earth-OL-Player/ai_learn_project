package com.earth.online.player.ailearn.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性。
 */
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;
    private long expiresInSeconds = 7200;

    /**
     * 获取 JWT 密钥。
     *
     * @return JWT 密钥
     */
    public String getSecret() {
        return secret;
    }

    /**
     * 设置 JWT 密钥。
     *
     * @param secret JWT 密钥
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * 获取过期秒数。
     *
     * @return 过期秒数
     */
    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    /**
     * 设置过期秒数。
     *
     * @param expiresInSeconds 过期秒数
     */
    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
