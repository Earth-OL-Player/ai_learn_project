package com.earth.online.player.ailearn.admin.interfaces;

import java.time.OffsetDateTime;

/**
 * 管理员用户响应。
 *
 * @param id 用户ID
 * @param username 用户名
 * @param nickname 昵称
 * @param email 邮箱
 * @param avatar 头像
 * @param superAdmin 是否超级管理员
 * @param experience 经验值
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record AdminUserResponse(
        String id,
        String username,
        String nickname,
        String email,
        String avatar,
        boolean superAdmin,
        int experience,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
