package com.earth.online.player.ailearn.question.infrastructure;

import java.time.LocalDateTime;

/**
 * 个人题目查询投影。
 */
public class MyQuestionRecord extends QuestionListRecord {

    private String content;
    private String standardAnswer;
    private String analysis;
    private LocalDateTime updatedAt;

    /** 获取题目内容。 */
    public String getContent() { return content; }

    /** 设置题目内容。 */
    public void setContent(String content) { this.content = content; }

    /** 获取参考答案。 */
    public String getStandardAnswer() { return standardAnswer; }

    /** 设置参考答案。 */
    public void setStandardAnswer(String standardAnswer) { this.standardAnswer = standardAnswer; }

    /** 获取解析。 */
    public String getAnalysis() { return analysis; }

    /** 设置解析。 */
    public void setAnalysis(String analysis) { this.analysis = analysis; }

    /** 获取更新时间。 */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** 设置更新时间。 */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
