package com.earth.online.player.ailearn.practice.interfaces;

import com.earth.online.player.ailearn.growth.interfaces.GrowthResponse;

/**
 * 刷题聊天响应。
 *
 * @param action 响应动作
 * @param phase 当前阶段
 * @param message 回复消息
 * @param question 题目
 * @param grading 评分结果
 * @param growth 成长概览
 */
public record PracticeMessageResponse(
        String action,
        String phase,
        String message,
        PracticeQuestionResponse question,
        PracticeGradingResponse grading,
        GrowthResponse growth
) {
}
