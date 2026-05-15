package com.earth.online.player.ailearn.answer.infrastructure;

/**
 * 答题记录持久化对象。
 */
public class AnswerRecordEntity {

    private Long id;
    private Long userId;
    private Long questionId;
    private Long sessionId;
    private String userAnswer;
    private Integer score;
    private Boolean correct;
    private String aiFeedback;
    private String gradingSource;
    private String improvementAdvice;
    private Integer durationSeconds;
    private Boolean firstAttempt;

    /** 获取答题记录ID。 */
    public Long getId() { return id; }

    /** 设置答题记录ID。 */
    public void setId(Long id) { this.id = id; }

    /** 获取用户ID。 */
    public Long getUserId() { return userId; }

    /** 设置用户ID。 */
    public void setUserId(Long userId) { this.userId = userId; }

    /** 获取题目ID。 */
    public Long getQuestionId() { return questionId; }

    /** 设置题目ID。 */
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    /** 获取会话ID。 */
    public Long getSessionId() { return sessionId; }

    /** 设置会话ID。 */
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    /** 获取用户答案。 */
    public String getUserAnswer() { return userAnswer; }

    /** 设置用户答案。 */
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    /** 获取得分。 */
    public Integer getScore() { return score; }

    /** 设置得分。 */
    public void setScore(Integer score) { this.score = score; }

    /** 获取是否基本正确。 */
    public Boolean getCorrect() { return correct; }

    /** 设置是否基本正确。 */
    public void setCorrect(Boolean correct) { this.correct = correct; }

    /** 获取结构化反馈JSON。 */
    public String getAiFeedback() { return aiFeedback; }

    /** 设置结构化反馈JSON。 */
    public void setAiFeedback(String aiFeedback) { this.aiFeedback = aiFeedback; }

    /** 获取评分来源。 */
    public String getGradingSource() { return gradingSource; }

    /** 设置评分来源。 */
    public void setGradingSource(String gradingSource) { this.gradingSource = gradingSource; }

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
}
