package com.earth.online.player.ailearn.analysis.interfaces;

/**
 * 薄弱知识点响应。
 *
 * @param knowledgePointId 知识点ID
 * @param knowledgePointName 知识点名称
 * @param answeredCount 答题次数
 * @param averageScore 平均分
 * @param lowScoreCount 低分次数
 * @param recommendedQuestionId 建议复习题目ID
 * @param recommendedQuestionTitle 建议复习题目标题
 * @param advice 学习建议
 */
public record WeakPointResponse(
        String knowledgePointId,
        String knowledgePointName,
        long answeredCount,
        double averageScore,
        long lowScoreCount,
        String recommendedQuestionId,
        String recommendedQuestionTitle,
        String advice
) {
}
