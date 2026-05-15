package com.earth.online.player.ailearn.agent.interfaces;

import java.util.List;

/**
 * 刷题题目响应。
 *
 * @param sessionId 会话ID
 * @param questionId 题目ID
 * @param title 题目标题
 * @param content 题目内容
 * @param questionType 题型
 * @param questionTypeText 题型文案
 * @param difficulty 难度
 * @param difficultyText 难度文案
 * @param knowledgePoints 知识点名称列表
 * @param sourceType 来源类型
 * @param recommendReason 推荐原因
 */
public record PracticeQuestionResponse(
        String sessionId,
        String questionId,
        String title,
        String content,
        String questionType,
        String questionTypeText,
        String difficulty,
        String difficultyText,
        List<String> knowledgePoints,
        String sourceType,
        String recommendReason
) {
}
