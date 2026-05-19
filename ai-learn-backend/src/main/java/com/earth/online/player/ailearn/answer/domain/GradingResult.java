package com.earth.online.player.ailearn.answer.domain;

import java.util.List;

/**
 * 答案评分结果。
 *
 * @param score 得分
 * @param hitPoints 命中点
 * @param missingPoints 缺失点
 * @param problems 问题点
 * @param referenceAnswer 参考答案
 * @param improvementAdvice 改进建议
 * @param reviewKnowledgePoints 建议复习知识点
 */
public record GradingResult(
        int score,
        List<String> hitPoints,
        List<String> missingPoints,
        List<String> problems,
        String referenceAnswer,
        String improvementAdvice,
        List<String> reviewKnowledgePoints
) {
}
