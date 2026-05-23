package com.earth.online.player.ailearn.common.security;

/**
 * JWT 解析结果。
 *
 * @param user 认证用户
 * @param tokenId JWT 唯一标识
 * @param expiresAt 过期时间戳秒数
 */
public record JwtParseResult(AuthenticatedUser user, String tokenId, long expiresAt) {
}
