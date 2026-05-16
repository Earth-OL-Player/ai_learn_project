package com.earth.online.player.ailearn.question.infrastructure;

/**
 * 系统题目写入记录。
 */
public class SystemQuestionWriteRecord {

    private Long id;
    private String code;
    private String question;
    private String questionType;
    private String standardAnswer;
    private Integer importanceScore;
    private Integer occurrenceCount;

    /** 获取题目ID。 */
    public Long getId() { return id; }

    /** 设置题目ID。 */
    public void setId(Long id) { this.id = id; }

    /** 获取题目编码。 */
    public String getCode() { return code; }

    /** 设置题目编码。 */
    public void setCode(String code) { this.code = code; }

    /** 获取题目内容。 */
    public String getQuestion() { return question; }

    /** 设置题目内容。 */
    public void setQuestion(String question) { this.question = question; }

    /** 获取题目分类。 */
    public String getQuestionType() { return questionType; }

    /** 设置题目分类。 */
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    /** 获取参考答案。 */
    public String getStandardAnswer() { return standardAnswer; }

    /** 设置参考答案。 */
    public void setStandardAnswer(String standardAnswer) { this.standardAnswer = standardAnswer; }

    /** 获取重要性评分。 */
    public Integer getImportanceScore() { return importanceScore; }

    /** 设置重要性评分。 */
    public void setImportanceScore(Integer importanceScore) { this.importanceScore = importanceScore; }

    /** 获取真实面试出现次数。 */
    public Integer getOccurrenceCount() { return occurrenceCount; }

    /** 设置真实面试出现次数。 */
    public void setOccurrenceCount(Integer occurrenceCount) { this.occurrenceCount = occurrenceCount; }
}
