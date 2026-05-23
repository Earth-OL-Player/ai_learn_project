package com.earth.online.player.ailearn.practice.application;

import com.earth.online.player.ailearn.answer.domain.GradingResult;
import com.earth.online.player.ailearn.growth.application.GrowthAwardService;
import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;
import com.earth.online.player.ailearn.growth.infrastructure.GrowthMapper;
import com.earth.online.player.ailearn.growth.interfaces.BadgeResponse;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeMapper;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeQuestionRecord;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeStatRecord;
import com.earth.online.player.ailearn.practice.interfaces.PracticeGradingResponse;
import com.earth.online.player.ailearn.user.infrastructure.UserMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 刷题成长经验结算服务。
 */
@Service
public class PracticeGrowthSettlementService {

    private final PracticeMapper practiceMapper;
    private final GrowthMapper growthMapper;
    private final UserMapper userMapper;
    private final GrowthAwardService growthAwardService;

    /**
     * 创建刷题成长经验结算服务。
     *
     * @param practiceMapper 刷题仓储
     * @param growthMapper 成长仓储
     * @param userMapper 用户仓储
     * @param growthAwardService 勋章发放服务
     */
    public PracticeGrowthSettlementService(
            PracticeMapper practiceMapper,
            GrowthMapper growthMapper,
            UserMapper userMapper,
            GrowthAwardService growthAwardService) {
        this.practiceMapper = practiceMapper;
        this.growthMapper = growthMapper;
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
    public PracticeGradingResponse settleAnswer(
            Long userId,
            PracticeQuestionRecord question,
            GradingResult gradingResult,
            boolean fallbackUsed) {
        PracticeStatRecord oldStat = practiceMapper.findStat(userId, question.getCode());
        int previousBest = oldStat == null || oldStat.getBestScore() == null ? 0 : oldStat.getBestScore();
        int previousLast = oldStat == null || oldStat.getLastScore() == null ? 0 : oldStat.getLastScore();
        int score = Math.max(0, Math.min(100, gradingResult.score()));
        int earnedExperience = Math.max(0, score - previousBest);
        practiceMapper.upsertStat(userId, question.getCode(), score);

        // 经验值按所有题目最高分总和重算，避免重复答题造成累计偏差。
        int totalExperience = Math.max(0, growthMapper.sumBestScores(userId));
        GrowthLevel level = GrowthLevel.resolveByExperience(totalExperience);
        GrowthRank rank = GrowthRank.resolveByExperience(totalExperience);
        userMapper.updateGrowth(userId, totalExperience, level.code(), rank.code());
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
                oldStat == null ? null : previousLast,
                buildExperienceDetail(oldStat == null, previousBest, previousLast, score, earnedExperience),
                totalExperience,
                newBadges,
                fallbackUsed
        );
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
