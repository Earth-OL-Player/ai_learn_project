package com.earth.online.player.ailearn.question.infrastructure;

/**
 * 题目新增持久化对象。
 */
public class QuestionInsertRecord {

    private Long id;
    private Long ownerUserId;
    private String title;
    private String content;
    private String questionType;
    private String difficulty;
    private String tags;
    private String standardAnswer;
    private String analysis;
    private String sourceType;

    /** 获取题目ID。 */
    public Long getId() { return id; }

    /** 设置题目ID。 */
    public void setId(Long id) { this.id = id; }

    /** 获取所属用户ID。 */
    public Long getOwnerUserId() { return ownerUserId; }

    /** 设置所属用户ID。 */
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }

    /** 获取标题。 */
    public String getTitle() { return title; }

    /** 设置标题。 */
    public void setTitle(String title) { this.title = title; }

    /** 获取内容。 */
    public String getContent() { return content; }

    /** 设置内容。 */
    public void setContent(String content) { this.content = content; }

    /** 获取题型。 */
    public String getQuestionType() { return questionType; }

    /** 设置题型。 */
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    /** 获取难度。 */
    public String getDifficulty() { return difficulty; }

    /** 设置难度。 */
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    /** 获取标签JSON。 */
    public String getTags() { return tags; }

    /** 设置标签JSON。 */
    public void setTags(String tags) { this.tags = tags; }

    /** 获取参考答案。 */
    public String getStandardAnswer() { return standardAnswer; }

    /** 设置参考答案。 */
    public void setStandardAnswer(String standardAnswer) { this.standardAnswer = standardAnswer; }

    /** 获取解析。 */
    public String getAnalysis() { return analysis; }

    /** 设置解析。 */
    public void setAnalysis(String analysis) { this.analysis = analysis; }

    /** 获取来源类型。 */
    public String getSourceType() { return sourceType; }

    /** 设置来源类型。 */
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
}
