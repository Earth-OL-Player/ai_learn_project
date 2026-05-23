package com.earth.online.player.ailearn.practice.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeMapper;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeQuestionRecord;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 刷题抽题策略服务。
 */
@Service
public class QuestionSelectionService {

    private final PracticeMapper practiceMapper;

    /**
     * 创建刷题抽题策略服务。
     *
     * @param practiceMapper 刷题仓储
     */
    public QuestionSelectionService(PracticeMapper practiceMapper) {
        this.practiceMapper = practiceMapper;
    }

    /**
     * 查询题目分类。
     *
     * @return 分类列表
     */
    public List<String> findQuestionTypes() {
        return practiceMapper.findQuestionTypes();
    }

    /**
     * 选择题目。
     *
     * @param userId 用户ID
     * @param questionTypes 题目分类
     * @return 题目记录
     */
    public PracticeQuestionRecord selectQuestion(Long userId, List<String> questionTypes) {
        List<PracticeQuestionRecord> candidates = practiceMapper.findCandidates(userId, questionTypes);
        if (candidates.isEmpty()) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "当前分类下暂无可用题目，请先导入系统题库");
        }

        // 在基础权重上引入小幅随机，避免用户连续刷到完全固定的题目顺序。
        PracticeQuestionRecord selected = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (PracticeQuestionRecord candidate : candidates) {
            double weightedScore = calculateWeight(candidate) * ThreadLocalRandom.current().nextDouble(0.75D, 1.35D);
            if (weightedScore > bestScore) {
                bestScore = weightedScore;
                selected = candidate;
            }
        }
        return selected == null ? candidates.get(0) : selected;
    }

    /**
     * 合并用户输入中的分类和显式选择分类。
     *
     * @param content 用户输入
     * @param selectedTypes 已选择分类
     * @return 分类列表
     */
    public List<String> mergeRequestedTypes(String content, List<String> selectedTypes) {
        List<String> availableTypes = practiceMapper.findQuestionTypes();
        List<String> normalizedSelected = normalizeTypes(selectedTypes);
        List<String> mentionedTypes = availableTypes.stream()
                .filter(type -> content.toUpperCase(Locale.ROOT).contains(type.toUpperCase(Locale.ROOT)))
                .map(String::trim)
                .toList();
        return mentionedTypes.isEmpty() ? normalizedSelected : mentionedTypes;
    }

    /**
     * 规整题目分类。
     *
     * @param questionTypes 原始分类
     * @return 分类列表
     */
    public List<String> normalizeTypes(List<String> questionTypes) {
        if (questionTypes == null || questionTypes.isEmpty()) {
            return Collections.emptyList();
        }

        // 分类名称保持原始大小写，只剔除空值和重复项。
        return questionTypes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * 计算抽题权重。
     *
     * @param question 候选题目
     * @return 权重
     */
    private double calculateWeight(PracticeQuestionRecord question) {
        int answeredCount = safeInt(question.getAnsweredCount());
        int bestScore = safeInt(question.getBestScore());
        double importanceScore = safeDouble(question.getImportanceScore());
        int occurrenceCount = safeInt(question.getOccurrenceCount());

        // 次数越少、重要性越高、历史得分越低，权重越高。
        double weight = 1.0D;
        weight += (importanceScore / 100.0D) * 4.0D;
        weight += 3.0D / (1 + answeredCount);
        weight += bestScore == 0 ? 2.5D : ((100 - bestScore) / 100.0D) * 3.0D;
        weight += Math.min(1.5D, Math.log10(occurrenceCount + 1.0D) / 2.0D);
        return Math.max(0.1D, weight);
    }

    /**
     * 获取安全整数。
     *
     * @param value 原始值
     * @return 安全值
     */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 将可空小数转换为安全值。
     *
     * @param value 原始值
     * @return 安全值
     */
    private double safeDouble(BigDecimal value) {
        return value == null ? 0.0D : value.doubleValue();
    }
}
