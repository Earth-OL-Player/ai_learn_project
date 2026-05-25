package com.earth.online.player.ailearn.user.infrastructure;

import java.time.OffsetDateTime;

/**
 * 用户刷题汇总查询记录。
 */
public class UserQuestionStatsRecord {

    private String questionCode;
    private String question;
    private String questionType;
    private Integer answerCount;
    private Integer bestScore;
    private Integer lastScore;
    private OffsetDateTime firstAnsweredAt;
    private OffsetDateTime lastAnsweredAt;

    /**
     * 获取题目编码。
     *
     * @return 题目编码
     */
    public String getQuestionCode() {
        return questionCode;
    }

    /**
     * 设置题目编码。
     *
     * @param questionCode 题目编码
     */
    public void setQuestionCode(String questionCode) {
        this.questionCode = questionCode;
    }

    /**
     * 获取题目内容。
     *
     * @return 题目内容
     */
    public String getQuestion() {
        return question;
    }

    /**
     * 设置题目内容。
     *
     * @param question 题目内容
     */
    public void setQuestion(String question) {
        this.question = question;
    }

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
     * 获取答题次数。
     *
     * @return 答题次数
     */
    public Integer getAnswerCount() {
        return answerCount;
    }

    /**
     * 设置答题次数。
     *
     * @param answerCount 答题次数
     */
    public void setAnswerCount(Integer answerCount) {
        this.answerCount = answerCount;
    }

    /**
     * 获取历史最高分。
     *
     * @return 历史最高分
     */
    public Integer getBestScore() {
        return bestScore;
    }

    /**
     * 设置历史最高分。
     *
     * @param bestScore 历史最高分
     */
    public void setBestScore(Integer bestScore) {
        this.bestScore = bestScore;
    }

    /**
     * 获取最近一次得分。
     *
     * @return 最近一次得分
     */
    public Integer getLastScore() {
        return lastScore;
    }

    /**
     * 设置最近一次得分。
     *
     * @param lastScore 最近一次得分
     */
    public void setLastScore(Integer lastScore) {
        this.lastScore = lastScore;
    }

    /**
     * 获取首次答题时间。
     *
     * @return 首次答题时间
     */
    public OffsetDateTime getFirstAnsweredAt() {
        return firstAnsweredAt;
    }

    /**
     * 设置首次答题时间。
     *
     * @param firstAnsweredAt 首次答题时间
     */
    public void setFirstAnsweredAt(OffsetDateTime firstAnsweredAt) {
        this.firstAnsweredAt = firstAnsweredAt;
    }

    /**
     * 获取最近答题时间。
     *
     * @return 最近答题时间
     */
    public OffsetDateTime getLastAnsweredAt() {
        return lastAnsweredAt;
    }

    /**
     * 设置最近答题时间。
     *
     * @param lastAnsweredAt 最近答题时间
     */
    public void setLastAnsweredAt(OffsetDateTime lastAnsweredAt) {
        this.lastAnsweredAt = lastAnsweredAt;
    }
}
