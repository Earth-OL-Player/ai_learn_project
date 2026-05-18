package com.earth.online.player.ailearn.admin.interfaces;

/**
 * 用户数量限制请求。
 *
 * @param maxUsers 最大用户数
 */
public record UserLimitRequest(Integer maxUsers) {
}
