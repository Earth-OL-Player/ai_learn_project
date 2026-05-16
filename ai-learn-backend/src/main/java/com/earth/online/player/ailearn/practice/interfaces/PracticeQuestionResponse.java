package com.earth.online.player.ailearn.practice.interfaces;

/**
 * 刷题题目响应。
 *
 * @param code 题目编码
 * @param question 题目内容
 * @param questionType 题目分类
 * @param importanceScore 重要性评分
 * @param occurrenceCount 真实面试出现次数
 * @param answeredCount 当前用户答题次数
 * @param bestScore 当前用户历史最高分
 */
public record PracticeQuestionResponse(
        String code,
        String question,
        String questionType,
        Integer importanceScore,
        Integer occurrenceCount,
        Integer answeredCount,
        Integer bestScore
) {
}
