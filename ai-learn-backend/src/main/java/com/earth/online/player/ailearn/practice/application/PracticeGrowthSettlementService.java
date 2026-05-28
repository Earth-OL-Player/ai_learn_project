package com.earth.online.player.ailearn.practice.application;

import com.earth.online.player.ailearn.answer.domain.GradingResult;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthMessages;
import com.earth.online.player.ailearn.common.util.NumberUtils;
import com.earth.online.player.ailearn.growth.application.GrowthAwardService;
import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;
import com.earth.online.player.ailearn.growth.interfaces.BadgeResponse;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeMapper;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeQuestionRecord;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeStatRecord;
import com.earth.online.player.ailearn.practice.interfaces.PracticeGradingResponse;
import com.earth.online.player.ailearn.user.domain.User;
import com.earth.online.player.ailearn.user.infrastructure.UserMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 刷题成长经验结算服务。
 */
@Service
public class PracticeGrowthSettlementService {

    private final PracticeMapper practiceMapper;
    private final UserMapper userMapper;
    private final GrowthAwardService growthAwardService;

    /**
     * 创建刷题成长经验结算服务。
     *
     * @param practiceMapper 刷题仓储
     * @param userMapper 用户仓储
     * @param growthAwardService 勋章发放服务
     */
    public PracticeGrowthSettlementService(
            PracticeMapper practiceMapper,
            UserMapper userMapper,
            GrowthAwardService growthAwardService) {
        this.practiceMapper = practiceMapper;
        this.userMapper = userMapper;
        this.growthAwardService = growthAwardService;
    }

    /**
     * 记录汇总并构建评分响应。
     *
     * @param userId 用户ID
     * @param question 题目
     * @param gradingResult 评分结果
     * @param fallbackUsed 是否使用兜底评分
     * @return 评分响应
     */
    @Transactional(rollbackFor = Exception.class)
    public PracticeGradingResponse settleAnswer(
            Long userId,
            PracticeQuestionRecord question,
            GradingResult gradingResult,
            boolean fallbackUsed) {
        User user = findLockedUser(userId);
        PracticeStatRecord oldStat = findLockedStat(userId, question.getCode());
        boolean firstAnswer = oldStat.getAnswerCount() == null || oldStat.getAnswerCount() == 0;
        int previousBest = NumberUtils.toIntOrZero(oldStat.getBestScore());
        int previousLast = NumberUtils.toIntOrZero(oldStat.getLastScore());
        int score = NumberUtils.clampPercentScore(gradingResult.score());
        int earnedExperience = Math.max(0, score - previousBest);
        practiceMapper.updateLockedStatAfterAnswer(oldStat.getId(), score);

        // 总经验直接基于用户表快照叠加本次突破分，避免扫描用户题目汇总表。
        int totalExperience = calculateTotalExperience(user, earnedExperience);
        refreshGrowthSnapshotIfNeeded(user, totalExperience, earnedExperience);
        List<BadgeResponse> newBadges = growthAwardService.awardAfterAnswer(userId, score);
        return new PracticeGradingResponse(
                score,
                gradingResult.hitPoints(),
                gradingResult.missingPoints(),
                gradingResult.problems(),
                gradingResult.referenceAnswer(),
                gradingResult.improvementAdvice(),
                earnedExperience,
                previousBest,
                firstAnswer ? null : previousLast,
                buildExperienceDetail(firstAnswer, previousBest, previousLast, score, earnedExperience),
                totalExperience,
                newBadges,
                fallbackUsed
        );
    }

    /**
     * 加锁读取当前用户。
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    private User findLockedUser(Long userId) {
        User user = userMapper.findByIdForUpdate(userId);
        if (user == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), AuthMessages.SESSION_INVALID_MESSAGE);
        }
        return user;
    }

    /**
     * 加锁读取当前题汇总。
     *
     * @param userId 用户ID
     * @param questionCode 题目编码
     * @return 题目汇总
     */
    private PracticeStatRecord findLockedStat(Long userId, String questionCode) {
        practiceMapper.insertEmptyStatIfAbsent(userId, questionCode);
        PracticeStatRecord stat = practiceMapper.findStatForUpdate(userId, questionCode);
        if (stat == null) {
            throw new BusinessException(ResponseCode.SYSTEM_ERROR.code(), "题目汇总更新失败，请稍后重试");
        }
        return stat;
    }

    /**
     * 计算答题后的用户总经验。
     *
     * @param user 用户信息
     * @param earnedExperience 本次新增经验
     * @return 答题后的总经验
     */
    private int calculateTotalExperience(User user, int earnedExperience) {
        int currentExperience = NumberUtils.toNonNegativeInt(user.getExperience());
        return currentExperience + earnedExperience;
    }

    /**
     * 必要时刷新用户成长快照。
     *
     * @param user 用户信息
     * @param totalExperience 答题后的总经验
     * @param earnedExperience 本次新增经验
     */
    private void refreshGrowthSnapshotIfNeeded(User user, int totalExperience, int earnedExperience) {
        GrowthLevel level = GrowthLevel.resolveByExperience(totalExperience);
        GrowthRank rank = GrowthRank.resolveByExperience(totalExperience);
        if (earnedExperience == 0 && isSameGrowthSnapshot(user, totalExperience, level, rank)) {
            return;
        }

        // 只在经验或成长编码变化时写用户表，减少无收益重复答题产生的写压力。
        userMapper.updateGrowth(user.getId(), totalExperience, level.code(), rank.code());
        user.setExperience(totalExperience);
        user.setLevelCode(level.code());
        user.setRankCode(rank.code());
    }

    /**
     * 判断用户成长快照是否已经一致。
     *
     * @param user 用户信息
     * @param totalExperience 当前总经验
     * @param level 当前等级
     * @param rank 当前段位
     * @return 是否一致
     */
    private boolean isSameGrowthSnapshot(User user, int totalExperience, GrowthLevel level, GrowthRank rank) {
        return Integer.valueOf(totalExperience).equals(user.getExperience())
                && level.code().equals(user.getLevelCode())
                && rank.code().equals(user.getRankCode());
    }

    /**
     * 构造经验变化说明。
     *
     * @param firstAnswer 是否首次答题
     * @param previousBest 评分前历史最高分
     * @param previousLast 评分前最近一次得分
     * @param score 本次得分
     * @param earnedExperience 本次新增经验
     * @return 经验变化说明
     */
    private String buildExperienceDetail(boolean firstAnswer, int previousBest, int previousLast, int score, int earnedExperience) {
        if (firstAnswer) {
            return "首次回答得了 " + score + " 分，新增 " + earnedExperience + " 经验。";
        }
        if (earnedExperience > 0) {
            return "比上次回答多拿了 " + Math.max(0, score - previousLast) + " 分，并突破历史最高分新增 " + earnedExperience + " 经验。";
        }
        return "未能突破上次最高分 " + previousBest + " 分，本次经验不变。";
    }
}
