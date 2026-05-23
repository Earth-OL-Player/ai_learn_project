package com.earth.online.player.ailearn.question.interfaces.admin;

/**
 * 系统题库导入字段差异。
 *
 * @param fieldName 字段名
 * @param fieldLabel 字段中文名
 * @param oldValue 写库前内容
 * @param newValue CSV内容
 */
public record ImportSystemQuestionDiffResponse(
        String fieldName,
        String fieldLabel,
        String oldValue,
        String newValue
) {
}
