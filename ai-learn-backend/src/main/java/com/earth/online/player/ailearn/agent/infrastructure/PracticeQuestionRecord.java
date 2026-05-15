package com.earth.online.player.ailearn.agent.infrastructure;

/**
 * 刷题题目查询投影。
 */
public class PracticeQuestionRecord {

    private Long id;
    private String title;
    private String content;
    private String questionType;
    private String difficulty;
    private String standardAnswer;
    private String knowledgePointNames;
    private String sourceType;
    private Long ownerUserId;

    /** 获取题目ID。 */
    public Long getId() { return id; }

    /** 设置题目ID。 */
    public void setId(Long id) { this.id = id; }

    /** 获取题目标题。 */
    public String getTitle() { return title; }

    /** 设置题目标题。 */
    public void setTitle(String title) { this.title = title; }

    /** 获取题目内容。 */
    public String getContent() { return content; }

    /** 设置题目内容。 */
    public void setContent(String content) { this.content = content; }

    /** 获取题型。 */
    public String getQuestionType() { return questionType; }

    /** 设置题型。 */
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    /** 获取难度。 */
    public String getDifficulty() { return difficulty; }

    /** 设置难度。 */
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    /** 获取参考答案。 */
    public String getStandardAnswer() { return standardAnswer; }

    /** 设置参考答案。 */
    public void setStandardAnswer(String standardAnswer) { this.standardAnswer = standardAnswer; }

    /** 获取知识点名称聚合。 */
    public String getKnowledgePointNames() { return knowledgePointNames; }

    /** 设置知识点名称聚合。 */
    public void setKnowledgePointNames(String knowledgePointNames) { this.knowledgePointNames = knowledgePointNames; }

    /** 获取来源类型。 */
    public String getSourceType() { return sourceType; }

    /** 设置来源类型。 */
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    /** 获取所属用户ID。 */
    public Long getOwnerUserId() { return ownerUserId; }

    /** 设置所属用户ID。 */
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
}
