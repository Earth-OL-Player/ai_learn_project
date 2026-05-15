package com.earth.online.player.ailearn.question.interfaces;

/**
 * 导入结果响应。
 *
 * @param importedCount 导入数量
 * @param mode 导入模式
 */
public record ImportMyQuestionsResponse(int importedCount, String mode) {
}
