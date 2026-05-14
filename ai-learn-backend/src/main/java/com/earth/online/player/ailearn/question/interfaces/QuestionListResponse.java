package com.earth.online.player.ailearn.question.interfaces;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 题目列表响应对象。
 *
 * @param id 题目ID
 * @param title 题目标题
 * @param questionType 题型
 * @param questionTypeText 题型文案
 * @param difficulty 难度
 * @param difficultyText 难度文案
 * @param tags 标签列表
 * @param knowledgePoints 知识点名称列表
 * @param createdAt 创建时间
 */
public record QuestionListResponse(
        String id,
        String title,
        String questionType,
        String questionTypeText,
        String difficulty,
        String difficultyText,
        List<String> tags,
        List<String> knowledgePoints,
        OffsetDateTime createdAt
) {
}
