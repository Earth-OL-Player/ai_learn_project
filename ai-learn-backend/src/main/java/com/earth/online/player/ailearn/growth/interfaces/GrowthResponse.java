package com.earth.online.player.ailearn.growth.interfaces;

import java.util.List;

/**
 * 成长信息响应对象。
 *
 * @param earnedExperience 本次获得经验，查询接口为0
 * @param currentExperience 当前累计经验
 * @param level 等级展示编码
 * @param levelName 等级名称，当前等级体系不再使用名称
 * @param rank 段位名称
 * @param levelValue 等级数字
 * @param currentLevelExperience 当前等级起始总经验
 * @param nextLevelExperience 下一级所需总经验
 * @param levelProgressText 等级经验展示文案
 * @param answeredCount 累计答题数量
 * @param averageScore 平均得分
 * @param experienceToNextLevel 距离下一级经验
 * @param streakDays 连续学习天数
 * @param badges 徽章墙
 * @param newBadges 本次新获得徽章
 * @param recentEvents 最近成长事件
 */
public record GrowthResponse(
        int earnedExperience,
        int currentExperience,
        String level,
        String levelName,
        String rank,
        int levelValue,
        int currentLevelExperience,
        int nextLevelExperience,
        String levelProgressText,
        long answeredCount,
        double averageScore,
        int experienceToNextLevel,
        int streakDays,
        List<BadgeResponse> badges,
        List<BadgeResponse> newBadges,
        List<GrowthEventResponse> recentEvents
) {
}
