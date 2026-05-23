package com.earth.online.player.ailearn.question.interfaces.admin;

import java.util.List;

/**
 * 系统题库导入预检结果。
 *
 * @param totalCount CSV总行数
 * @param importableCount 可导入行数
 * @param createdCount 新增数量
 * @param updatedCount 更新数量
 * @param conflictCount 冲突数量
 * @param errorCount 错误数量
 * @param rows 预览行
 * @param issues 字段问题汇总
 */
public record ImportSystemQuestionsPrecheckResponse(
        int totalCount,
        int importableCount,
        int createdCount,
        int updatedCount,
        int conflictCount,
        int errorCount,
        List<ImportSystemQuestionPreviewRowResponse> rows,
        List<ImportSystemQuestionIssueResponse> issues
) {
}
