package com.earth.online.player.ailearn.question.infrastructure;

import java.time.LocalDateTime;

/**
 * 题目列表查询投影。
 */
public class QuestionListRecord {

    private Long id;
    private String title;
    private String questionType;
    private String difficulty;
    private String tags;
    private String knowledgePointNames;
    private LocalDateTime createdAt;

    /** 获取题目ID。 */
    public Long getId() { return id; }

    /** 设置题目ID。 */
    public void setId(Long id) { this.id = id; }

    /** 获取题目标题。 */
    public String getTitle() { return title; }

    /** 设置题目标题。 */
    public void setTitle(String title) { this.title = title; }

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

    /** 获取知识点名称聚合。 */
    public String getKnowledgePointNames() { return knowledgePointNames; }

    /** 设置知识点名称聚合。 */
    public void setKnowledgePointNames(String knowledgePointNames) { this.knowledgePointNames = knowledgePointNames; }

    /** 获取创建时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** 设置创建时间。 */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
