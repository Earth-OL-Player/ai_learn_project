package com.earth.online.player.ailearn.agent.interfaces;

import com.earth.online.player.ailearn.growth.interfaces.GrowthResponse;
import java.util.List;

/**
 * 提交答案响应。
 *
 * @param answerRecordId 答题记录ID
 * @param score 得分
 * @param isCorrect 是否基本正确
 * @param hitPoints 命中点
 * @param missingPoints 缺失点
 * @param problems 问题点
 * @param referenceAnswer 参考答案
 * @param improvementAdvice 改进建议
 * @param reviewKnowledgePoints 建议复习知识点
 * @param gradingSource 评分来源
 * @param growth 成长反馈
 */
public record PracticeSubmitResponse(
        String answerRecordId,
        int score,
        boolean isCorrect,
        List<String> hitPoints,
        List<String> missingPoints,
        List<String> problems,
        String referenceAnswer,
        String improvementAdvice,
        List<String> reviewKnowledgePoints,
        String gradingSource,
        GrowthResponse growth
) {
}
