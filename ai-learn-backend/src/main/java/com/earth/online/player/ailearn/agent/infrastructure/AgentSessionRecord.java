package com.earth.online.player.ailearn.agent.infrastructure;

import java.time.LocalDateTime;

/**
 * 刷题会话持久化对象。
 */
public class AgentSessionRecord {

    private Long id;
    private Long userId;
    private Long questionId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    /** 获取会话ID。 */
    public Long getId() { return id; }

    /** 设置会话ID。 */
    public void setId(Long id) { this.id = id; }

    /** 获取用户ID。 */
    public Long getUserId() { return userId; }

    /** 设置用户ID。 */
    public void setUserId(Long userId) { this.userId = userId; }

    /** 获取题目ID。 */
    public Long getQuestionId() { return questionId; }

    /** 设置题目ID。 */
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    /** 获取会话状态。 */
    public String getStatus() { return status; }

    /** 设置会话状态。 */
    public void setStatus(String status) { this.status = status; }

    /** 获取开始时间。 */
    public LocalDateTime getStartedAt() { return startedAt; }

    /** 设置开始时间。 */
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    /** 获取提交时间。 */
    public LocalDateTime getSubmittedAt() { return submittedAt; }

    /** 设置提交时间。 */
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
