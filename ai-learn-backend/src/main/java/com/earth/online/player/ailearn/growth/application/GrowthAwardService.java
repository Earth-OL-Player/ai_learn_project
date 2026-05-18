package com.earth.online.player.ailearn.growth.application;

import com.earth.online.player.ailearn.growth.domain.BadgeRule;
import com.earth.online.player.ailearn.growth.infrastructure.BadgeRecord;
import com.earth.online.player.ailearn.growth.infrastructure.GrowthMapper;
import com.earth.online.player.ailearn.growth.interfaces.BadgeResponse;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 成长徽章发放服务。
 */
@Service
public class GrowthAwardService {

    private static final LocalTime LATE_NIGHT_START = LocalTime.of(22, 0);
    private static final LocalTime EARLY_MORNING_START = LocalTime.of(6, 0);
    private static final LocalTime EARLY_MORNING_END = LocalTime.of(8, 0);

    private final GrowthMapper growthMapper;

    /**
     * 创建成长徽章服务。
     *
     * @param growthMapper 成长仓储
     */
    public GrowthAwardService(GrowthMapper growthMapper) {
        this.growthMapper = growthMapper;
    }

    /**
     * 答题后发放徽章。
     *
     * @param userId 用户ID
     * @param score 本次得分
     * @return 本次新获得徽章
     */
    public List<BadgeResponse> awardAfterAnswer(Long userId, int score) {
        // 刷题评分完成后统一按最新汇总数据判断所有非追问类勋章，不再写入成长明细流水。
        List<String> candidateRules = new ArrayList<>();
        appendAnswerCountRules(candidateRules, growthMapper.countCompletedAnswers(userId));
        appendLearningDayRules(candidateRules, calculateLearningDays(userId));
        appendRareTimeRules(candidateRules, LocalDateTime.now());
        return awardBadges(userId, candidateRules);
    }

    /**
     * 讨论追问后发放勋章。
     *
     * @param userId 用户ID
     * @param followUpCount 当前题评分后连续追问次数
     * @return 本次新获得徽章
     */
    public List<BadgeResponse> awardAfterDiscussion(Long userId, int followUpCount) {
        if (followUpCount < 3) {
            return List.of();
        }

        // 问到底是隐藏稀有勋章，达到三次有效追问后按幂等方式尝试发放。
        return awardBadges(userId, List.of(BadgeRule.ASK_TO_END.ruleCode()));
    }

    /**
     * 查询徽章墙。
     *
     * @param userId 用户ID
     * @return 徽章墙
     */
    public List<BadgeResponse> findBadgeWall(Long userId) {
        return growthMapper.findBadgeWall(userId).stream()
                .filter(this::isConfiguredBadge)
                .filter(this::isVisibleOnWall)
                .sorted(Comparator.comparingInt(record -> BadgeRule.orderOf(record.getRuleCode())))
                .map(this::toBadgeResponse)
                .toList();
    }

    /**
     * 计算总学习天数。
     *
     * @param userId 用户ID
     * @return 总学习天数
     */
    public int calculateLearningDays(Long userId) {
        return growthMapper.countLearningDays(userId);
    }

    /**
     * 发放候选徽章。
     *
     * @param userId 用户ID
     * @param ruleCodes 规则编码
     * @return 新发放徽章
     */
    private List<BadgeResponse> awardBadges(Long userId, List<String> ruleCodes) {
        List<BadgeResponse> newBadges = new ArrayList<>();
        for (String ruleCode : ruleCodes) {
            int affected = growthMapper.insertUserBadge(userId, ruleCode);
            if (affected > 0) {
                BadgeRecord badge = growthMapper.findBadgeByRuleCode(userId, ruleCode);
                if (badge == null) {
                    continue;
                }
                BadgeResponse response = toBadgeResponse(badge);
                newBadges.add(response);
            }
        }
        return newBadges;
    }

    /**
     * 追加累计完成题数规则。
     *
     * @param candidateRules 候选规则
     * @param completedAnswers 累计完成次数
     */
    private void appendAnswerCountRules(List<String> candidateRules, long completedAnswers) {
        if (completedAnswers >= 1) {
            candidateRules.add(BadgeRule.FIRST_ANSWER.ruleCode());
        }
        if (completedAnswers >= 10) {
            candidateRules.add(BadgeRule.ANSWER_10.ruleCode());
        }
        if (completedAnswers >= 100) {
            candidateRules.add(BadgeRule.ANSWER_100.ruleCode());
        }
        if (completedAnswers >= 300) {
            candidateRules.add(BadgeRule.ANSWER_300.ruleCode());
        }
    }

    /**
     * 追加总学习天数规则。
     *
     * @param candidateRules 候选规则
     * @param learningDays 总学习天数
     */
    private void appendLearningDayRules(List<String> candidateRules, int learningDays) {
        if (learningDays >= 3) {
            candidateRules.add(BadgeRule.LEARNING_3_DAYS.ruleCode());
        }
        if (learningDays >= 30) {
            candidateRules.add(BadgeRule.LEARNING_30_DAYS.ruleCode());
        }
        if (learningDays >= 100) {
            candidateRules.add(BadgeRule.LEARNING_100_DAYS.ruleCode());
        }
    }

    /**
     * 追加隐藏时段类规则。
     *
     * @param candidateRules 候选规则
     * @param completedAt 完成时间
     */
    private void appendRareTimeRules(List<String> candidateRules, LocalDateTime completedAt) {
        LocalTime completedTime = completedAt.toLocalTime();
        if (!completedTime.isBefore(LATE_NIGHT_START)) {
            candidateRules.add(BadgeRule.LATE_NIGHT.ruleCode());
        }
        if (!completedTime.isBefore(EARLY_MORNING_START) && completedTime.isBefore(EARLY_MORNING_END)) {
            candidateRules.add(BadgeRule.EARLY_MORNING.ruleCode());
        }
        if (isWeekend(completedAt)) {
            candidateRules.add(BadgeRule.WEEKEND_PRACTICE.ruleCode());
        }
    }

    /**
     * 判断是否周末完成刷题。
     *
     * @param completedAt 完成时间
     * @return 是否周末
     */
    private boolean isWeekend(LocalDateTime completedAt) {
        DayOfWeek dayOfWeek = completedAt.getDayOfWeek();
        return DayOfWeek.SATURDAY.equals(dayOfWeek) || DayOfWeek.SUNDAY.equals(dayOfWeek);
    }

    /**
     * 判断是否为本期配置勋章。
     *
     * @param record 徽章记录
     * @return 是否配置内勋章
     */
    private boolean isConfiguredBadge(BadgeRecord record) {
        return BadgeRule.fromRuleCode(record.getRuleCode()).isPresent();
    }

    /**
     * 判断徽章是否应展示在个人中心。
     *
     * @param record 徽章记录
     * @return 是否展示
     */
    private boolean isVisibleOnWall(BadgeRecord record) {
        BadgeRule rule = BadgeRule.fromRuleCode(record.getRuleCode()).orElseThrow();
        return !rule.hidden() || Boolean.TRUE.equals(record.getAcquired());
    }

    /**
     * 转换徽章响应。
     *
     * @param record 徽章记录
     * @return 徽章响应
     */
    private BadgeResponse toBadgeResponse(BadgeRecord record) {
        BadgeRule rule = BadgeRule.fromRuleCode(record.getRuleCode()).orElseThrow();
        return new BadgeResponse(
                String.valueOf(record.getId()),
                record.getName(),
                record.getDescription(),
                record.getIcon(),
                record.getRuleCode(),
                rule.category(),
                rule.categoryName(),
                rule.hidden(),
                Boolean.TRUE.equals(record.getAcquired()),
                record.getAcquiredAt() == null ? null : record.getAcquiredAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
        );
    }
}

