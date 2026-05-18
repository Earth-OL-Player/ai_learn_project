package com.earth.online.player.ailearn.admin.interfaces;

/**
 * 用户数量限制响应。
 *
 * @param maxUsers 最大用户数
 * @param currentUsers 当前用户数
 */
public record UserLimitResponse(int maxUsers, long currentUsers) {
}
