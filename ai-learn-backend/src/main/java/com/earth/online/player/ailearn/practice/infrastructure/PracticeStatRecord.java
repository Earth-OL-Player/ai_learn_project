package com.earth.online.player.ailearn.practice.infrastructure;

/**
 * 用户题目汇总记录。
 */
public class PracticeStatRecord {

    private Long id;
    private Long userId;
    private String questionCode;
    private Integer answerCount;
    private Integer bestScore;
    private Integer lastScore;

    /** 获取汇总ID。 */
    public Long getId() { return id; }

    /** 设置汇总ID。 */
    public void setId(Long id) { this.id = id; }

    /** 获取用户ID。 */
    public Long getUserId() { return userId; }

    /** 设置用户ID。 */
    public void setUserId(Long userId) { this.userId = userId; }

    /** 获取题目编码。 */
    public String getQuestionCode() { return questionCode; }

    /** 设置题目编码。 */
    public void setQuestionCode(String questionCode) { this.questionCode = questionCode; }

    /** 获取答题次数。 */
    public Integer getAnswerCount() { return answerCount; }

    /** 设置答题次数。 */
    public void setAnswerCount(Integer answerCount) { this.answerCount = answerCount; }

    /** 获取历史最高分。 */
    public Integer getBestScore() { return bestScore; }

    /** 设置历史最高分。 */
    public void setBestScore(Integer bestScore) { this.bestScore = bestScore; }

    /** 获取最近得分。 */
    public Integer getLastScore() { return lastScore; }

    /** 设置最近得分。 */
    public void setLastScore(Integer lastScore) { this.lastScore = lastScore; }
}
