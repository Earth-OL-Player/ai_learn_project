package com.earth.online.player.ailearn.question.interfaces.admin;

import java.time.OffsetDateTime;

/**
 * 系统题目响应。
 *
 * @param id 题目ID
 * @param code 题目编码
 * @param question 题目内容
 * @param questionType 题目分类
 * @param standardAnswer 参考答案
 * @param importanceScore 重要性评分
 * @param occurrenceCount 真实面试出现次数
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record SystemQuestionResponse(
        String id,
        String code,
        String question,
        String questionType,
        String standardAnswer,
        Integer importanceScore,
        Integer occurrenceCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
