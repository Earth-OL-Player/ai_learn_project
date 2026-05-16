package com.earth.online.player.ailearn.question.infrastructure;

import java.time.LocalDateTime;

/**
 * 题目详情查询投影。
 */
public class QuestionDetailRecord extends QuestionListRecord {

    private String standardAnswer;
    private LocalDateTime updatedAt;

    /** 获取参考答案。 */
    public String getStandardAnswer() { return standardAnswer; }

    /** 设置参考答案。 */
    public void setStandardAnswer(String standardAnswer) { this.standardAnswer = standardAnswer; }

    /** 获取更新时间。 */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** 设置更新时间。 */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
