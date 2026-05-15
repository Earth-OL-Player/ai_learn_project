package com.earth.online.player.ailearn.growth.application;

import com.earth.online.player.ailearn.growth.infrastructure.BadgeRecord;
import com.earth.online.player.ailearn.growth.infrastructure.GrowthEventRecord;
import com.earth.online.player.ailearn.growth.infrastructure.GrowthMapper;
import com.earth.online.player.ailearn.growth.interfaces.BadgeResponse;
import com.earth.online.player.ailearn.growth.interfaces.GrowthEventResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 成长徽章发放服务。
 */
@Service
public class GrowthAwardService {

    private static final String FIRST_ANSWER = "FIRST_ANSWER";
    private static final String STREAK_3_DAYS = "STREAK_3_DAYS";
    private static final String STREAK_7_DAYS = "STREAK_7_DAYS";
    private static final String HIGH_SCORE = "HIGH_SCORE";
    private static final String SCORE_IMPROVED = "SCORE_IMPROVED";
    private static final String RAG_10 = "RAG_10";
    private static final String AGENT_10 = "AGENT_10";

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
     * 答题后发放经验事件和徽章。
     *
     * @param userId 用户ID
     * @param score 本次得分
     * @param earnedExperience 本次经验
     * @param answeredCount 累计答题数
     * @param improved 是否低分复刷提分
     * @return 本次新获得徽章
     */
    public List<BadgeResponse> awardAfterAnswer(
            Long userId,
            int score,
            int earnedExperience,
            long answeredCount,
            boolean improved) {
        growthMapper.insertGrowthEvent(
                userId,
                "ANSWER",
                "完成一次刷题",
                "本次得分 " + score + " 分，获得 " + earnedExperience + " 经验",
                earnedExperience
        );

        List<String> candidateRules = new ArrayList<>();
        if (answeredCount == 1) {
            candidateRules.add(FIRST_ANSWER);
        }
        if (score >= 90) {
            candidateRules.add(HIGH_SCORE);
        }
        if (improved) {
            candidateRules.add(SCORE_IMPROVED);
        }
        int streakDays = calculateStreakDays(userId);
        if (streakDays >= 3) {
            candidateRules.add(STREAK_3_DAYS);
        }
        if (streakDays >= 7) {
            candidateRules.add(STREAK_7_DAYS);
        }
        if (growthMapper.countAnsweredByKnowledgeKeyword(userId, "RAG") >= 10) {
            candidateRules.add(RAG_10);
        }
        if (growthMapper.countAnsweredByKnowledgeKeyword(userId, "Agent") >= 10) {
            candidateRules.add(AGENT_10);
        }
        return awardBadges(userId, candidateRules);
    }

    /**
     * 查询徽章墙。
     *
     * @param userId 用户ID
     * @return 徽章墙
     */
    public List<BadgeResponse> findBadgeWall(Long userId) {
        return growthMapper.findBadgeWall(userId).stream()
                .map(this::toBadgeResponse)
                .toList();
    }

    /**
     * 查询最近成长事件。
     *
     * @param userId 用户ID
     * @return 成长事件
     */
    public List<GrowthEventResponse> findRecentEvents(Long userId) {
        return growthMapper.findRecentEvents(userId, 8).stream()
                .map(this::toEventResponse)
                .toList();
    }

    /**
     * 计算连续学习天数。
     *
     * @param userId 用户ID
     * @return 连续学习天数
     */
    public int calculateStreakDays(Long userId) {
        List<LocalDate> dates = growthMapper.findRecentAnswerDates(userId);
        if (dates.isEmpty()) {
            return 0;
        }
        int streak = 0;
        LocalDate expected = LocalDate.now();
        for (LocalDate date : dates) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (streak == 0 && date.equals(expected.minusDays(1))) {
                streak++;
                expected = date.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
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
                BadgeResponse response = toBadgeResponse(badge);
                newBadges.add(response);
                growthMapper.insertGrowthEvent(userId, "BADGE", "获得徽章：" + response.name(), response.description(), 0);
            }
        }
        return newBadges;
    }

    /**
     * 转换徽章响应。
     *
     * @param record 徽章记录
     * @return 徽章响应
     */
    private BadgeResponse toBadgeResponse(BadgeRecord record) {
        return new BadgeResponse(
                String.valueOf(record.getId()),
                record.getName(),
                record.getDescription(),
                record.getIcon(),
                record.getRuleCode(),
                Boolean.TRUE.equals(record.getAcquired()),
                record.getAcquiredAt() == null ? null : record.getAcquiredAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
        );
    }

    /**
     * 转换成长事件响应。
     *
     * @param record 事件记录
     * @return 事件响应
     */
    private GrowthEventResponse toEventResponse(GrowthEventRecord record) {
        return new GrowthEventResponse(
                String.valueOf(record.getId()),
                record.getEventType(),
                record.getTitle(),
                record.getDescription(),
                record.getExperienceDelta() == null ? 0 : record.getExperienceDelta(),
                record.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
        );
    }
}
