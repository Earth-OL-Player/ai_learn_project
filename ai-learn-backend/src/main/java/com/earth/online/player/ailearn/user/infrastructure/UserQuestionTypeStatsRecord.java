package com.earth.online.player.ailearn.user.infrastructure;

import java.math.BigDecimal;

/**
 * 用户题型刷题汇总查询记录。
 */
public class UserQuestionTypeStatsRecord {

    private String questionType;
    private Long questionCount;
    private Long answerCount;
    private BigDecimal averageBestScore;
    private BigDecimal averageLastScore;
    private Long weakCount;

    /**
     * 获取题目类型。
     *
     * @return 题目类型
     */
    public String getQuestionType() {
        return questionType;
    }

    /**
     * 设置题目类型。
     *
     * @param questionType 题目类型
     */
    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    /**
     * 获取已练题目数。
     *
     * @return 已练题目数
     */
    public Long getQuestionCount() {
        return questionCount;
    }

    /**
     * 设置已练题目数。
     *
     * @param questionCount 已练题目数
     */
    public void setQuestionCount(Long questionCount) {
        this.questionCount = questionCount;
    }

    /**
     * 获取累计答题次数。
     *
     * @return 累计答题次数
     */
    public Long getAnswerCount() {
        return answerCount;
    }

    /**
     * 设置累计答题次数。
     *
     * @param answerCount 累计答题次数
     */
    public void setAnswerCount(Long answerCount) {
        this.answerCount = answerCount;
    }

    /**
     * 获取平均最高分。
     *
     * @return 平均最高分
     */
    public BigDecimal getAverageBestScore() {
        return averageBestScore;
    }

    /**
     * 设置平均最高分。
     *
     * @param averageBestScore 平均最高分
     */
    public void setAverageBestScore(BigDecimal averageBestScore) {
        this.averageBestScore = averageBestScore;
    }

    /**
     * 获取平均最近得分。
     *
     * @return 平均最近得分
     */
    public BigDecimal getAverageLastScore() {
        return averageLastScore;
    }

    /**
     * 设置平均最近得分。
     *
     * @param averageLastScore 平均最近得分
     */
    public void setAverageLastScore(BigDecimal averageLastScore) {
        this.averageLastScore = averageLastScore;
    }

    /**
     * 获取薄弱题目数。
     *
     * @return 薄弱题目数
     */
    public Long getWeakCount() {
        return weakCount;
    }

    /**
     * 设置薄弱题目数。
     *
     * @param weakCount 薄弱题目数
     */
    public void setWeakCount(Long weakCount) {
        this.weakCount = weakCount;
    }
}
