package com.earth.online.player.ailearn.user.interfaces;

import java.math.BigDecimal;

/**
 * 用户题型刷题汇总响应。
 *
 * @param questionType 题目类型
 * @param questionCount 已练题目数
 * @param answerCount 累计答题次数
 * @param averageBestScore 平均最高分
 * @param averageLastScore 平均最近得分
 * @param weakCount 薄弱题目数
 */
public record UserQuestionTypeStatsResponse(
        String questionType,
        long questionCount,
        long answerCount,
        BigDecimal averageBestScore,
        BigDecimal averageLastScore,
        long weakCount
) {
}
