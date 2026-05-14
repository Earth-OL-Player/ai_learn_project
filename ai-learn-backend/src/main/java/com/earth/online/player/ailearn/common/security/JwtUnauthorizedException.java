package com.earth.online.player.ailearn.common.security;

/**
 * JWT 认证异常。
 */
public class JwtUnauthorizedException extends RuntimeException {

    /**
     * 创建 JWT 认证异常。
     *
     * @param message 错误消息
     */
    public JwtUnauthorizedException(String message) {
        super(message);
    }
}
