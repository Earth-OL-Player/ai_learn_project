package com.earth.online.player.ailearn.practice.infrastructure;

import com.earth.online.player.ailearn.answer.domain.GradingResult;

/**
 * AI 服务评分结果包装。
 *
 * @param gradingResult 评分结果
 * @param fallbackUsed 是否使用兜底评分
 */
public record PracticeAiGradingResult(
        GradingResult gradingResult,
        boolean fallbackUsed
) {
}
