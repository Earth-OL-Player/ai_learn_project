package com.earth.online.player.ailearn.agent.interfaces;

import java.util.List;

/**
 * 开始刷题请求。
 *
 * @param difficulty 难度
 * @param questionType 题型
 * @param knowledgePointIds 知识点ID列表
 * @param sourceScope 题库范围：DEFAULT、MINE、MIXED
 */
public record StartPracticeRequest(String difficulty, String questionType, List<String> knowledgePointIds, String sourceScope) {
}
