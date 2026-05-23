package com.earth.online.player.ailearn.answer.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 本地规则答案评分领域服务。
 */
@Service
public class AnswerGradingDomainService implements AnswerGradingPort {

    private static final int MAX_KEYWORD_COUNT = 8;
    private static final int SHORT_ANSWER_LENGTH = 20;
    private static final int MAX_SCORE = 100;
    private static final int CONTENT_BASE_SCORE = 20;
    private static final int KEYWORD_SCORE_WEIGHT = 80;
    private static final String SPLIT_REGEX = "[\\s，。、；：,.!?！？（）()\"'“”‘’]+";
    private static final Pattern ASCII_TERM_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9+_.-]{1,}");
    private static final Set<String> ASCII_IGNORED_TERMS = Set.of("query", "rewrite");
    private static final List<String> DOMAIN_TERMS = List.of(
            "RAG", "Embedding", "Chunk", "Query Rewrite", "BM25", "Rerank", "Prompt", "Fine-tuning",
            "检索增强生成", "离线建库", "在线问答", "文档解析", "权限过滤", "查询改写", "问题理解", "向量检索",
            "混合检索", "关键词检索", "向量库", "向量化", "召回", "重排", "精排", "证据", "引用", "拒答",
            "上下文压缩", "评测闭环", "反馈闭环", "可追溯", "知识更新", "微调", "输出风格", "格式遵循"
    );
    private static final Map<String, List<String>> KEYWORD_ALIASES = Map.ofEntries(
            Map.entry("rag", List.of("检索增强生成", "检索增强", "外部知识", "知识库问答")),
            Map.entry("检索增强生成", List.of("RAG", "检索增强", "外部知识", "知识库问答")),
            Map.entry("embedding", List.of("向量化", "向量表示", "嵌入模型", "转成向量")),
            Map.entry("chunk", List.of("切分", "分块", "切片", "文本块", "知识片段")),
            Map.entry("query rewrite", List.of("查询改写", "问题改写", "改写问题")),
            Map.entry("bm25", List.of("关键词检索", "稀疏检索", "混合检索")),
            Map.entry("rerank", List.of("重排", "精排", "重新排序")),
            Map.entry("prompt", List.of("提示词", "上下文")),
            Map.entry("fine-tuning", List.of("微调", "模型微调")),
            Map.entry("向量化", List.of("Embedding", "嵌入", "向量表示")),
            Map.entry("重排", List.of("Rerank", "精排", "排序")),
            Map.entry("查询改写", List.of("Query Rewrite", "问题改写", "改写问题")),
            Map.entry("离线建库", List.of("文档解析", "清洗", "切分", "向量库", "入库")),
            Map.entry("在线问答", List.of("用户提问", "检索召回", "召回", "生成答案"))
    );

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
        String normalizedAnswer = normalizeText(safeAnswer);
        for (String keyword : keywords) {
            if (isKeywordHit(keyword, normalizedAnswer)) {
                hitPoints.add("已覆盖「" + keyword + "」相关核心要点");
            } else {
                missingPoints.add("待补充「" + keyword + "」相关说明");
            }
        }

        int score = calculateScore(hitPoints.size(), keywords.size(), safeAnswer.length());
        List<String> problems = buildProblems(safeAnswer, hitPoints.isEmpty());
        String advice = buildAdvice(missingPoints, problems);

        return new GradingResult(
                score,
                hitPoints,
                missingPoints,
                problems,
                standardAnswer,
                advice
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
     * @param excludedSources 题目分类等不参与扣分的来源
     * @return 关键词集合
     */
    private Set<String> buildKeywords(String standardAnswer, List<String> excludedSources) {
        Set<String> keywords = new LinkedHashSet<>();
        if (StringUtils.hasText(standardAnswer)) {
            addAsciiTerms(keywords, standardAnswer);
            addDomainTerms(keywords, standardAnswer);
            addSplitTokens(keywords, standardAnswer);
        }
        return limitKeywords(filterExcludedKeywords(keywords, excludedSources));
    }

    /**
     * 提取英文技术词。
     *
     * @param keywords 关键词集合
     * @param source 标准答案
     */
    private void addAsciiTerms(Set<String> keywords, String source) {
        Matcher matcher = ASCII_TERM_PATTERN.matcher(source);
        while (matcher.find()) {
            String term = matcher.group();
            if (!ASCII_IGNORED_TERMS.contains(term.toLowerCase(Locale.ROOT))) {
                addKeyword(keywords, term);
            }
        }
    }

    /**
     * 从标准答案中提取常见领域短语。
     *
     * @param keywords 关键词集合
     * @param source 标准答案
     */
    private void addDomainTerms(Set<String> keywords, String source) {
        String normalizedSource = normalizeText(source);
        for (String term : DOMAIN_TERMS) {
            if (normalizedSource.contains(normalizeText(term))) {
                addKeyword(keywords, term);
            }
        }
    }

    /**
     * 按标点和空白切分补充短关键词。
     *
     * @param keywords 关键词集合
     * @param source 标准答案
     */
    private void addSplitTokens(Set<String> keywords, String source) {
        for (String token : source.split(SPLIT_REGEX)) {
            addKeyword(keywords, token);
        }
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
     * 过滤题目分类中已经包含的宽泛词，避免用户必须复述分类名。
     *
     * @param keywords 候选关键词
     * @param excludedSources 题目分类等排除来源
     * @return 过滤后的关键词
     */
    private Set<String> filterExcludedKeywords(Set<String> keywords, List<String> excludedSources) {
        if (excludedSources == null || excludedSources.isEmpty()) {
            return keywords;
        }
        Set<String> filtered = new LinkedHashSet<>();
        for (String keyword : keywords) {
            if (!isExcludedKeyword(keyword, excludedSources)) {
                filtered.add(keyword);
            }
        }
        return filtered;
    }

    /**
     * 判断关键词是否属于题目分类等非扣分来源。
     *
     * @param keyword 关键词
     * @param excludedSources 排除来源
     * @return 是否排除
     */
    private boolean isExcludedKeyword(String keyword, List<String> excludedSources) {
        String normalizedKeyword = normalizeText(keyword);
        return excludedSources.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeText)
                .anyMatch(source -> source.contains(normalizedKeyword) || normalizedKeyword.contains(source));
    }

    /**
     * 判断答案是否覆盖关键词或同义表达。
     *
     * @param keyword 关键词
     * @param normalizedAnswer 规整后的用户答案
     * @return 是否命中
     */
    private boolean isKeywordHit(String keyword, String normalizedAnswer) {
        String normalizedKeyword = normalizeText(keyword);
        if (normalizedAnswer.contains(normalizedKeyword)) {
            return true;
        }

        // 同义表达只用于放宽命中，不作为额外必答项。
        List<String> aliases = KEYWORD_ALIASES.getOrDefault(keyword.toLowerCase(Locale.ROOT), Collections.emptyList());
        return aliases.stream().anyMatch(alias -> normalizedAnswer.contains(normalizeText(alias)));
    }

    /**
     * 规整文本用于大小写和空白不敏感匹配。
     *
     * @param value 原始文本
     * @return 规整文本
     */
    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
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
            return "建议优先补充：" + String.join("；", toAdvicePoints(missingPoints)) + "。";
        }
        return String.join("；", problems) + "。";
    }

    /**
     * 转换缺失点为建议短句。
     *
     * @param missingPoints 缺失点
     * @return 建议短句列表
     */
    private List<String> toAdvicePoints(List<String> missingPoints) {
        return missingPoints.stream()
                .map(point -> point.replace("待补充「", "").replace("」相关说明", ""))
                .map(point -> "补充「" + point + "」")
                .toList();
    }

}
