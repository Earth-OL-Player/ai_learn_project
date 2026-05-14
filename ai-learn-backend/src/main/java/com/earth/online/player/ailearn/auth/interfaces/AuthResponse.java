package com.earth.online.player.ailearn.auth.interfaces;

import com.earth.online.player.ailearn.user.domain.UserSummary;

/**
 * 登录凭证响应。
 *
 * @param accessToken 访问令牌
 * @param tokenType 令牌类型
 * @param expiresIn 过期秒数
 * @param user 用户摘要
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserSummary user) {
}
