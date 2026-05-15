package com.earth.online.player.ailearn.answer.domain;

import java.util.List;

/**
 * 答案评分端口。
 */
public interface AnswerGradingPort {

    /**
     * 对用户答案评分。
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @param questionContent 题目内容
     * @param standardAnswer 标准答案
     * @param knowledgePoints 知识点
     * @param userAnswer 用户答案
     * @return 评分结果
     */
    GradingResult grade(
            Long userId,
            Long questionId,
            String questionContent,
            String standardAnswer,
            List<String> knowledgePoints,
            String userAnswer
    );
}
