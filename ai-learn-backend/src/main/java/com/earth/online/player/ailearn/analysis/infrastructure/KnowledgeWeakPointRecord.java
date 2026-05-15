package com.earth.online.player.ailearn.analysis.infrastructure;

/**
 * 知识点分析查询投影。
 */
public class KnowledgeWeakPointRecord {

    private Long knowledgePointId;
    private String knowledgePointName;
    private Long answeredCount;
    private Double averageScore;
    private Long lowScoreCount;
    private Long recommendedQuestionId;
    private String recommendedQuestionTitle;

    /** 获取知识点ID。 */
    public Long getKnowledgePointId() { return knowledgePointId; }

    /** 设置知识点ID。 */
    public void setKnowledgePointId(Long knowledgePointId) { this.knowledgePointId = knowledgePointId; }

    /** 获取知识点名称。 */
    public String getKnowledgePointName() { return knowledgePointName; }

    /** 设置知识点名称。 */
    public void setKnowledgePointName(String knowledgePointName) { this.knowledgePointName = knowledgePointName; }

    /** 获取答题次数。 */
    public Long getAnsweredCount() { return answeredCount; }

    /** 设置答题次数。 */
    public void setAnsweredCount(Long answeredCount) { this.answeredCount = answeredCount; }

    /** 获取平均分。 */
    public Double getAverageScore() { return averageScore; }

    /** 设置平均分。 */
    public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }

    /** 获取低分次数。 */
    public Long getLowScoreCount() { return lowScoreCount; }

    /** 设置低分次数。 */
    public void setLowScoreCount(Long lowScoreCount) { this.lowScoreCount = lowScoreCount; }

    /** 获取推荐题目ID。 */
    public Long getRecommendedQuestionId() { return recommendedQuestionId; }

    /** 设置推荐题目ID。 */
    public void setRecommendedQuestionId(Long recommendedQuestionId) { this.recommendedQuestionId = recommendedQuestionId; }

    /** 获取推荐题目标题。 */
    public String getRecommendedQuestionTitle() { return recommendedQuestionTitle; }

    /** 设置推荐题目标题。 */
    public void setRecommendedQuestionTitle(String recommendedQuestionTitle) { this.recommendedQuestionTitle = recommendedQuestionTitle; }
}
