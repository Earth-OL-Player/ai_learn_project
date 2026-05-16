package com.earth.online.player.ailearn.question.interfaces;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 题目列表响应对象。
 *
 * @param id 题目ID
 * @param code 题目编码
 * @param question 题目内容
 * @param questionType 题目分类
 * @param questionTypeText 分类文案
 * @param knowledgePoints 知识点名称列表
 * @param importanceScore 重要性评分
 * @param occurrenceCount 真实面试出现次数
 * @param createdAt 创建时间
 */
public record QuestionListResponse(
        String id,
        String code,
        String question,
        String questionType,
        String questionTypeText,
        List<String> knowledgePoints,
        Integer importanceScore,
        Integer occurrenceCount,
        OffsetDateTime createdAt
) {
}
