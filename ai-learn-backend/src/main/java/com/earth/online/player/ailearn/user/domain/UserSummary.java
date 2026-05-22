package com.earth.online.player.ailearn.user.domain;

import java.time.OffsetDateTime;

/**
 * 当前用户摘要。
 *
 * @param id 用户ID
 * @param username 用户名
 * @param nickname 昵称
 * @param avatar 头像地址
 * @param gender 性别编码
 * @param email 邮箱
 * @param experience 经验值
 * @param level 等级展示编码
 * @param levelName 等级名称，当前等级体系不再使用名称
 * @param rank 段位名称
 * @param levelValue 等级数字
 * @param currentLevelExperience 当前等级起始总经验
 * @param nextLevelExperience 下一级所需总经验
 * @param experienceToNextLevel 距离下一级经验
 * @param levelProgressText 等级经验展示文案
 * @param superAdmin 是否超级管理员
 * @param createdAt 创建时间
 */
public record UserSummary(
        String id,
        String username,
        String nickname,
        String avatar,
        String gender,
        String email,
        Integer experience,
        String level,
        String levelName,
        String rank,
        Integer levelValue,
        Integer currentLevelExperience,
        Integer nextLevelExperience,
        Integer experienceToNextLevel,
        String levelProgressText,
        Boolean superAdmin,
        OffsetDateTime createdAt
) {
}
