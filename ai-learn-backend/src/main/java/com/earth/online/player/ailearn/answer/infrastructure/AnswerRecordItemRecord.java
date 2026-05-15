package com.earth.online.player.ailearn.answer.infrastructure;

import java.time.LocalDateTime;

/**
 * 答题记录列表查询投影。
 */
public class AnswerRecordItemRecord {

    private Long id;
    private Long questionId;
    private String questionTitle;
    private String questionType;
    private String difficulty;
    private Integer score;
    private Boolean correct;
    private String improvementAdvice;
    private Integer durationSeconds;
    private Boolean firstAttempt;
    private LocalDateTime createdAt;

    /** 获取记录ID。 */
    public Long getId() { return id; }

    /** 设置记录ID。 */
    public void setId(Long id) { this.id = id; }

    /** 获取题目ID。 */
    public Long getQuestionId() { return questionId; }

    /** 设置题目ID。 */
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    /** 获取题目标题。 */
    public String getQuestionTitle() { return questionTitle; }

    /** 设置题目标题。 */
    public void setQuestionTitle(String questionTitle) { this.questionTitle = questionTitle; }

    /** 获取题型。 */
    public String getQuestionType() { return questionType; }

    /** 设置题型。 */
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    /** 获取难度。 */
    public String getDifficulty() { return difficulty; }

    /** 设置难度。 */
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    /** 获取得分。 */
    public Integer getScore() { return score; }

    /** 设置得分。 */
    public void setScore(Integer score) { this.score = score; }

    /** 获取是否基本正确。 */
    public Boolean getCorrect() { return correct; }

    /** 设置是否基本正确。 */
    public void setCorrect(Boolean correct) { this.correct = correct; }

    /** 获取改进建议。 */
    public String getImprovementAdvice() { return improvementAdvice; }

    /** 设置改进建议。 */
    public void setImprovementAdvice(String improvementAdvice) { this.improvementAdvice = improvementAdvice; }

    /** 获取答题耗时。 */
    public Integer getDurationSeconds() { return durationSeconds; }

    /** 设置答题耗时。 */
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    /** 获取是否首次作答。 */
    public Boolean getFirstAttempt() { return firstAttempt; }

    /** 设置是否首次作答。 */
    public void setFirstAttempt(Boolean firstAttempt) { this.firstAttempt = firstAttempt; }

    /** 获取创建时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** 设置创建时间。 */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
