package com.earth.online.player.ailearn.practice.interfaces;

/**
 * 当前轮刷题聊天消息响应。
 *
 * @param role 消息角色
 * @param text 消息文本
 * @param question 题目卡片
 * @param grading 评分卡片
 */
public record PracticeChatMessageResponse(
        String role,
        String text,
        PracticeQuestionResponse question,
        PracticeGradingResponse grading
) {
}
