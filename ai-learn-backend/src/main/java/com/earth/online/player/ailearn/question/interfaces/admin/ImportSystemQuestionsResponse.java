package com.earth.online.player.ailearn.question.interfaces.admin;

/**
 * 系统题库导入结果。
 *
 * @param importedCount 导入总数
 * @param createdCount 新增数量
 * @param updatedCount 更新数量
 */
public record ImportSystemQuestionsResponse(int importedCount, int createdCount, int updatedCount) {
}
