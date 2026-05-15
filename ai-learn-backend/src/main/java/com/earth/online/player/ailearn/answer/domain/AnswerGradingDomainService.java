package com.earth.online.player.ailearn.answer.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 本地规则答案评分领域服务。
 */
@Service
public class AnswerGradingDomainService implements AnswerGradingPort {

    private static final int MAX_KEYWORD_COUNT = 8;
    private static final int SHORT_ANSWER_LENGTH = 20;
    private static final int CORRECT_SCORE = 60;
    private static final int MAX_SCORE = 100;
    private static final int CONTENT_BASE_SCORE = 20;
    private static final int KEYWORD_SCORE_WEIGHT = 80;
    private static final String SPLIT_REGEX = "[\\s，。、；：,.!?！？（）()\"'“”‘’]+";

    /**
     * 按关键词命中情况生成结构化评分。
     *
     * @param standardAnswer 标准答案
     * @param knowledgePoints 知识点名称
     * @param userAnswer 用户答案
     * @return 评分结果
     */
    public GradingResult grade(String standardAnswer, List<String> knowledgePoints, String userAnswer) {
        Set<String> keywords = buildKeywords(standardAnswer, knowledgePoints);
        String safeAnswer = userAnswer == null ? "" : userAnswer.trim();
        List<String> hitPoints = new ArrayList<>();
        List<String> missingPoints = new ArrayList<>();

        // 命中判断使用大小写不敏感策略，兼容英文技术词。
        String lowerAnswer = safeAnswer.toLowerCase(java.util.Locale.ROOT);
        for (String keyword : keywords) {
            if (lowerAnswer.contains(keyword.toLowerCase(java.util.Locale.ROOT))) {
                hitPoints.add("命中了「" + keyword + "」相关要点");
            } else {
                missingPoints.add("缺少「" + keyword + "」相关说明");
            }
        }

        int score = calculateScore(hitPoints.size(), keywords.size(), safeAnswer.length());
        List<String> problems = buildProblems(safeAnswer, hitPoints.isEmpty());
        String advice = buildAdvice(missingPoints, problems);
        List<String> reviewKnowledgePoints = missingPoints.isEmpty() ? knowledgePoints : extractReviewPoints(missingPoints);

        return new GradingResult(
                score,
                score >= CORRECT_SCORE,
                hitPoints,
                missingPoints,
                problems,
                standardAnswer,
                advice,
                reviewKnowledgePoints
        );
    }

    /**
     * 按评分端口协议执行本地规则评分。
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @param questionContent 题目内容
     * @param standardAnswer 标准答案
     * @param knowledgePoints 知识点名称
     * @param userAnswer 用户答案
     * @return 评分结果
     */
    @Override
    public GradingResult grade(
            Long userId,
            Long questionId,
            String questionContent,
            String standardAnswer,
            List<String> knowledgePoints,
            String userAnswer) {
        return grade(standardAnswer, knowledgePoints, userAnswer);
    }

    /**
     * 构建评分关键词集合。
     *
     * @param standardAnswer 标准答案
     * @param knowledgePoints 知识点名称
     * @return 关键词集合
     */
    private Set<String> buildKeywords(String standardAnswer, List<String> knowledgePoints) {
        Set<String> keywords = new LinkedHashSet<>();
        for (String point : knowledgePoints) {
            addKeyword(keywords, point);
        }
        if (StringUtils.hasText(standardAnswer)) {
            for (String token : standardAnswer.split(SPLIT_REGEX)) {
                addKeyword(keywords, token);
            }
        }
        return limitKeywords(keywords);
    }

    /**
     * 添加单个关键词。
     *
     * @param keywords 关键词集合
     * @param value 候选关键词
     */
    private void addKeyword(Set<String> keywords, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String keyword = value.trim();
        if (keyword.length() >= 2 && keyword.length() <= 20) {
            keywords.add(keyword);
        }
    }

    /**
     * 限制关键词数量，避免反馈过长。
     *
     * @param keywords 原始关键词
     * @return 裁剪后的关键词集合
     */
    private Set<String> limitKeywords(Set<String> keywords) {
        Set<String> limited = new LinkedHashSet<>();
        for (String keyword : keywords) {
            if (limited.size() >= MAX_KEYWORD_COUNT) {
                break;
            }
            limited.add(keyword);
        }
        return limited;
    }

    /**
     * 计算本地规则分数。
     *
     * @param hitCount 命中数量
     * @param totalCount 总关键词数量
     * @param answerLength 答案长度
     * @return 得分
     */
    private int calculateScore(int hitCount, int totalCount, int answerLength) {
        if (totalCount == 0) {
            return Math.min(MAX_SCORE, CONTENT_BASE_SCORE + Math.min(answerLength, KEYWORD_SCORE_WEIGHT));
        }
        int keywordScore = Math.round((hitCount * KEYWORD_SCORE_WEIGHT) / (float) totalCount);
        int contentScore = answerLength >= SHORT_ANSWER_LENGTH ? CONTENT_BASE_SCORE : answerLength;
        return Math.max(0, Math.min(MAX_SCORE, keywordScore + contentScore));
    }

    /**
     * 生成问题点。
     *
     * @param answer 用户答案
     * @param noKeywordHit 是否没有命中关键词
     * @return 问题点列表
     */
    private List<String> buildProblems(String answer, boolean noKeywordHit) {
        List<String> problems = new ArrayList<>();
        if (answer.length() < SHORT_ANSWER_LENGTH) {
            problems.add("回答较简略，建议补充关键流程和原因说明");
        }
        if (noKeywordHit) {
            problems.add("未明显覆盖标准答案中的核心关键词");
        }
        return problems;
    }

    /**
     * 生成改进建议。
     *
     * @param missingPoints 缺失点
     * @param problems 问题点
     * @return 改进建议
     */
    private String buildAdvice(List<String> missingPoints, List<String> problems) {
        if (missingPoints.isEmpty() && problems.isEmpty()) {
            return "整体回答较完整，建议继续补充工程化细节和实际案例。";
        }
        if (!missingPoints.isEmpty()) {
            return "建议优先补充：" + String.join("；", missingPoints) + "。";
        }
        return String.join("；", problems) + "。";
    }

    /**
     * 从缺失点中提取复习提示。
     *
     * @param missingPoints 缺失点
     * @return 复习知识点
     */
    private List<String> extractReviewPoints(List<String> missingPoints) {
        return missingPoints.stream()
                .map(point -> point.replace("缺少「", "").replace("」相关说明", ""))
                .toList();
    }
}
