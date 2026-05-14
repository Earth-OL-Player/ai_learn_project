package com.earth.online.player.ailearn.common.security;

/**
 * 当前认证用户。
 *
 * @param userId 用户ID
 * @param username 用户名
 */
public record AuthenticatedUser(Long userId, String username) {
}
