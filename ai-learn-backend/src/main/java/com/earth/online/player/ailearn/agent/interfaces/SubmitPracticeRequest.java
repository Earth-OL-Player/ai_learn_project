package com.earth.online.player.ailearn.agent.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 提交刷题答案请求。
 *
 * @param sessionId 会话ID
 * @param questionId 题目ID
 * @param userAnswer 用户答案
 * @param durationSeconds 答题耗时秒数
 */
public record SubmitPracticeRequest(
        @NotBlank(message = "会话ID不能为空") String sessionId,
        @NotBlank(message = "题目ID不能为空") String questionId,
        @NotBlank(message = "答案不能为空") String userAnswer,
        @PositiveOrZero(message = "答题耗时不能为负数") Integer durationSeconds
) {
}
