package com.earth.online.player.ailearn.question.interfaces;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 题目详情响应对象。
 *
 * @param id 题目ID
 * @param title 题目标题
 * @param content 题目内容
 * @param questionType 题型
 * @param questionTypeText 题型文案
 * @param difficulty 难度
 * @param difficultyText 难度文案
 * @param tags 标签列表
 * @param knowledgePoints 知识点名称列表
 * @param standardAnswer 参考答案
 * @param analysis 解析
 * @param createdAt 创建时间
 */
public record QuestionDetailResponse(
        String id,
        String title,
        String content,
        String questionType,
        String questionTypeText,
        String difficulty,
        String difficultyText,
        List<String> tags,
        List<String> knowledgePoints,
        String standardAnswer,
        String analysis,
        OffsetDateTime createdAt
) {
}
