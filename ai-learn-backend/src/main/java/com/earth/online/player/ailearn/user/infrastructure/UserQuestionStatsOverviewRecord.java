package com.earth.online.player.ailearn.user.infrastructure;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 用户刷题汇总概览查询记录。
 */
public class UserQuestionStatsOverviewRecord {

    private Long practicedQuestionCount;
    private Long totalAnswerCount;
    private BigDecimal averageBestScore;
    private BigDecimal averageLastScore;
    private Long weakQuestionCount;
    private OffsetDateTime lastAnsweredAt;

    /**
     * 获取已练题目数。
     *
     * @return 已练题目数
     */
    public Long getPracticedQuestionCount() {
        return practicedQuestionCount;
    }

    /**
     * 设置已练题目数。
     *
     * @param practicedQuestionCount 已练题目数
     */
    public void setPracticedQuestionCount(Long practicedQuestionCount) {
        this.practicedQuestionCount = practicedQuestionCount;
    }

    /**
     * 获取累计答题次数。
     *
     * @return 累计答题次数
     */
    public Long getTotalAnswerCount() {
        return totalAnswerCount;
    }

    /**
     * 设置累计答题次数。
     *
     * @param totalAnswerCount 累计答题次数
     */
    public void setTotalAnswerCount(Long totalAnswerCount) {
        this.totalAnswerCount = totalAnswerCount;
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
    public Long getWeakQuestionCount() {
        return weakQuestionCount;
    }

    /**
     * 设置薄弱题目数。
     *
     * @param weakQuestionCount 薄弱题目数
     */
    public void setWeakQuestionCount(Long weakQuestionCount) {
        this.weakQuestionCount = weakQuestionCount;
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
