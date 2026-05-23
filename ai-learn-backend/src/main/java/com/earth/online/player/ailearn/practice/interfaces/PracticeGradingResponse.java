package com.earth.online.player.ailearn.practice.interfaces;

import com.earth.online.player.ailearn.growth.interfaces.BadgeResponse;
import java.util.List;

/**
 * 刷题评分响应。
 *
 * @param score 得分
 * @param hitPoints 命中点
 * @param missingPoints 缺失点
 * @param problems 问题点
 * @param referenceAnswer 参考答案
 * @param improvementAdvice 优化建议
 * @param earnedExperience 本次获得经验
 * @param previousBestScore 评分前历史最高分
 * @param previousLastScore 评分前最近一次得分
 * @param experienceDetail 经验变化说明
 * @param totalExperience 当前总经验
 * @param newBadges 新获得徽章
 * @param fallbackUsed 是否使用兜底评分
 */
public record PracticeGradingResponse(
        Integer score,
        List<String> hitPoints,
        List<String> missingPoints,
        List<String> problems,
        String referenceAnswer,
        String improvementAdvice,
        Integer earnedExperience,
        Integer previousBestScore,
        Integer previousLastScore,
        String experienceDetail,
        Integer totalExperience,
        List<BadgeResponse> newBadges,
        Boolean fallbackUsed
) {
}
