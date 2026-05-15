package com.earth.online.player.ailearn.answer.interfaces;

import java.time.OffsetDateTime;

/**
 * 答题记录响应对象。
 *
 * @param id 记录ID
 * @param questionId 题目ID
 * @param questionTitle 题目标题
 * @param questionType 题型
 * @param questionTypeText 题型文案
 * @param difficulty 难度
 * @param difficultyText 难度文案
 * @param score 得分
 * @param isCorrect 是否基本正确
 * @param improvementAdvice 改进建议
 * @param durationSeconds 答题耗时
 * @param firstAttempt 是否首次作答
 * @param createdAt 答题时间
 */
public record AnswerRecordResponse(
        String id,
        String questionId,
        String questionTitle,
        String questionType,
        String questionTypeText,
        String difficulty,
        String difficultyText,
        int score,
        boolean isCorrect,
        String improvementAdvice,
        Integer durationSeconds,
        boolean firstAttempt,
        OffsetDateTime createdAt
) {
}
