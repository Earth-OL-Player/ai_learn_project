package com.earth.online.player.ailearn.user.interfaces;

import java.time.OffsetDateTime;

/**
 * 用户刷题记录列表响应项。
 *
 * @param questionCode 题目编码
 * @param question 题目内容
 * @param questionType 题目类型
 * @param answerCount 答题次数
 * @param bestScore 历史最高分
 * @param lastScore 最近一次得分
 * @param firstAnsweredAt 首次答题时间
 * @param lastAnsweredAt 最近答题时间
 */
public record UserQuestionStatsItemResponse(
        String questionCode,
        String question,
        String questionType,
        Integer answerCount,
        Integer bestScore,
        Integer lastScore,
        OffsetDateTime firstAnsweredAt,
        OffsetDateTime lastAnsweredAt
) {
}
