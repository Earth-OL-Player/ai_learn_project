package com.earth.online.player.ailearn.user.interfaces;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 用户刷题记录概览响应。
 *
 * @param practicedQuestionCount 已练题目数
 * @param totalAnswerCount 累计答题次数
 * @param averageBestScore 平均最高分
 * @param averageLastScore 平均最近得分
 * @param weakQuestionCount 薄弱题目数
 * @param lastAnsweredAt 最近答题时间
 * @param questionTypes 已练题型列表
 * @param typeStats 题型维度汇总
 */
public record UserQuestionStatsOverviewResponse(
        long practicedQuestionCount,
        long totalAnswerCount,
        BigDecimal averageBestScore,
        BigDecimal averageLastScore,
        long weakQuestionCount,
        OffsetDateTime lastAnsweredAt,
        List<String> questionTypes,
        List<UserQuestionTypeStatsResponse> typeStats
) {
}
