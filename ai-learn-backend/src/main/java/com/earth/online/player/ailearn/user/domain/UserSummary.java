package com.earth.online.player.ailearn.user.domain;

import java.time.OffsetDateTime;

/**
 * 当前用户摘要。
 *
 * @param id 用户ID
 * @param username 用户名
 * @param nickname 昵称
 * @param avatar 头像地址
 * @param email 邮箱
 * @param experience 经验值
 * @param level 等级展示编码
 * @param levelName 等级名称
 * @param rank 段位名称
 * @param superAdmin 是否超级管理员
 * @param createdAt 创建时间
 */
public record UserSummary(
        String id,
        String username,
        String nickname,
        String avatar,
        String email,
        Integer experience,
        String level,
        String levelName,
        String rank,
        Boolean superAdmin,
        OffsetDateTime createdAt
) {
}
