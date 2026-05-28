package com.earth.online.player.ailearn.growth.application;

import com.earth.online.player.ailearn.common.util.NumberUtils;
import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;
import com.earth.online.player.ailearn.growth.infrastructure.GrowthMapper;
import com.earth.online.player.ailearn.growth.interfaces.BadgeResponse;
import com.earth.online.player.ailearn.growth.interfaces.GrowthResponse;
import com.earth.online.player.ailearn.user.application.CurrentUserService;
import com.earth.online.player.ailearn.user.domain.User;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 成长信息应用服务。
 */
@Service
public class GrowthService {

    private final GrowthMapper growthMapper;
    private final GrowthRuleService growthRuleService;
    private final GrowthAwardService growthAwardService;
    private final CurrentUserService currentUserService;

    /**
     * 创建成长信息服务。
     *
     * @param growthMapper 成长仓储
     * @param growthRuleService 成长规则服务
     * @param growthAwardService 成长徽章服务
     * @param currentUserService 当前用户读取服务
     */
    public GrowthService(
            GrowthMapper growthMapper,
            GrowthRuleService growthRuleService,
            GrowthAwardService growthAwardService,
            CurrentUserService currentUserService) {
        this.growthMapper = growthMapper;
        this.growthRuleService = growthRuleService;
        this.growthAwardService = growthAwardService;
        this.currentUserService = currentUserService;
    }

    /**
     * 查询当前用户成长信息。
     *
     * @return 成长信息
     */
    public GrowthResponse getCurrentGrowth() {
        return getCurrentGrowth(Collections.emptyList());
    }

    /**
     * 查询当前用户成长信息并携带本次新勋章。
     *
     * @param newBadges 本次新获得勋章
     * @return 成长信息
     */
    public GrowthResponse getCurrentGrowth(List<BadgeResponse> newBadges) {
        User user = currentUserService.requireCurrentUser();
        return buildGrowthResponse(user, newBadges == null ? Collections.emptyList() : newBadges);
    }

    /**
     * 构造成长响应。
     *
     * @param user 用户信息
     * @param newBadges 本次新获得勋章
     * @return 成长响应
     */
    private GrowthResponse buildGrowthResponse(User user, List<BadgeResponse> newBadges) {
        // 总经验直接读取用户表快照，避免成长查询扫描用户题目汇总表。
        int experience = NumberUtils.toNonNegativeInt(user.getExperience());
        GrowthLevel level = growthRuleService.resolveLevel(experience);
        GrowthRank rank = growthRuleService.resolveRank(experience);
        int nextLevelExperience = level.nextLevelExperience();
        int currentLevelExperience = level.minExperience();
        return new GrowthResponse(
                0,
                experience,
                level.displayCode(),
                level.displayName(),
                rank.displayName(),
                level.levelValue(),
                currentLevelExperience,
                nextLevelExperience,
                level.progressText(experience),
                growthMapper.countCompletedAnswers(user.getId()),
                growthMapper.averageBestScore(user.getId()),
                Math.max(0, nextLevelExperience - experience),
                growthAwardService.calculateLearningDays(user.getId()),
                growthAwardService.findBadgeWall(user.getId()),
                newBadges
        );
    }
}

