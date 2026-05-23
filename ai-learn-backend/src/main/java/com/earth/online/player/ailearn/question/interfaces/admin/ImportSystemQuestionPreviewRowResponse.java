package com.earth.online.player.ailearn.question.interfaces.admin;

import java.math.BigDecimal;
import java.util.List;

/**
 * 系统题库导入预览行。
 *
 * @param rowIndex CSV行号
 * @param action 导入动作
 * @param actionText 导入动作中文说明
 * @param importable 是否可导入
 * @param code 题目编码
 * @param question 题目内容
 * @param questionType 题目分类
 * @param standardAnswer 参考答案
 * @param importanceScore 重要性评分
 * @param occurrenceCount 真实面试出现次数
 * @param diffs 字段差异
 * @param issues 字段问题
 */
public record ImportSystemQuestionPreviewRowResponse(
        int rowIndex,
        String action,
        String actionText,
        boolean importable,
        String code,
        String question,
        String questionType,
        String standardAnswer,
        BigDecimal importanceScore,
        Integer occurrenceCount,
        List<ImportSystemQuestionDiffResponse> diffs,
        List<ImportSystemQuestionIssueResponse> issues
) {
}
