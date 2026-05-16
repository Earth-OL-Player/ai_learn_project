package com.earth.online.player.ailearn.practice.interfaces;

import com.earth.online.player.ailearn.growth.interfaces.BadgeResponse;
import java.util.List;

/**
 * 刷题评分响应。
 *
 * @param score 得分
 * @param correct 是否基本正确
 * @param hitPoints 命中点
 * @param missingPoints 缺失点
 * @param problems 问题点
 * @param referenceAnswer 参考答案
 * @param improvementAdvice 优化建议
 * @param reviewKnowledgePoints 建议复习点
 * @param earnedExperience 本次获得经验
 * @param totalExperience 当前总经验
 * @param newBadges 新获得徽章
 */
public record PracticeGradingResponse(
        Integer score,
        Boolean correct,
        List<String> hitPoints,
        List<String> missingPoints,
        List<String> problems,
        String referenceAnswer,
        String improvementAdvice,
        List<String> reviewKnowledgePoints,
        Integer earnedExperience,
        Integer totalExperience,
        List<BadgeResponse> newBadges
) {
}
