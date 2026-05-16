package com.earth.online.player.ailearn.practice.infrastructure;

/**
 * 当前刷题状态记录。
 */
public class PracticeSessionRecord {

    private Long id;
    private Long userId;
    private String questionCode;
    private String phase;
    private Integer lastScore;

    /** 获取状态ID。 */
    public Long getId() { return id; }

    /** 设置状态ID。 */
    public void setId(Long id) { this.id = id; }

    /** 获取用户ID。 */
    public Long getUserId() { return userId; }

    /** 设置用户ID。 */
    public void setUserId(Long userId) { this.userId = userId; }

    /** 获取题目编码。 */
    public String getQuestionCode() { return questionCode; }

    /** 设置题目编码。 */
    public void setQuestionCode(String questionCode) { this.questionCode = questionCode; }

    /** 获取阶段。 */
    public String getPhase() { return phase; }

    /** 设置阶段。 */
    public void setPhase(String phase) { this.phase = phase; }

    /** 获取最近得分。 */
    public Integer getLastScore() { return lastScore; }

    /** 设置最近得分。 */
    public void setLastScore(Integer lastScore) { this.lastScore = lastScore; }
}
