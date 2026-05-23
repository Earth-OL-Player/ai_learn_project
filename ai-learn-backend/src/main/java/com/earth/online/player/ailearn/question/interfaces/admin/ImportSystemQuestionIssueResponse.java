package com.earth.online.player.ailearn.question.interfaces.admin;

/**
 * 系统题库导入字段问题。
 *
 * @param rowIndex CSV行号
 * @param fieldName 字段名
 * @param fieldLabel 字段中文名
 * @param message 问题说明
 */
public record ImportSystemQuestionIssueResponse(
        int rowIndex,
        String fieldName,
        String fieldLabel,
        String message
) {
}
