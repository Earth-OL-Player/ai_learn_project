package com.earth.online.player.ailearn.growth.application;

import com.earth.online.player.ailearn.answer.infrastructure.AnswerRecordMapper;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;
import com.earth.online.player.ailearn.growth.interfaces.GrowthResponse;
import com.earth.online.player.ailearn.user.domain.User;
import com.earth.online.player.ailearn.user.infrastructure.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 成长信息应用服务。
 */
@Service
public class GrowthService {

    private final UserMapper userMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final GrowthRuleService growthRuleService;
    private final GrowthAwardService growthAwardService;

    /**
     * 创建成长信息服务。
     *
     * @param userMapper 用户仓储
     * @param answerRecordMapper 答题记录仓储
     * @param growthRuleService 成长规则服务
     */
    public GrowthService(
            UserMapper userMapper,
            AnswerRecordMapper answerRecordMapper,
            GrowthRuleService growthRuleService,
            GrowthAwardService growthAwardService) {
        this.userMapper = userMapper;
        this.answerRecordMapper = answerRecordMapper;
        this.growthRuleService = growthRuleService;
        this.growthAwardService = growthAwardService;
    }

    /**
     * 查询当前用户成长信息。
     *
     * @return 成长信息
     */
    public GrowthResponse getCurrentGrowth() {
        AuthenticatedUser authenticatedUser = AuthContext.getUser();
        if (authenticatedUser == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录后即可使用该功能");
        }
        User user = userMapper.findById(authenticatedUser.userId());
        if (user == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录状态已失效，请重新登录");
        }

        // 成长查询聚合用户表和答题记录，供个人中心直接展示。
        int experience = Math.max(0, user.getExperience() == null ? 0 : user.getExperience());
        GrowthLevel level = growthRuleService.resolveLevel(experience);
        GrowthRank rank = growthRuleService.resolveRank(experience);
        int nextLevelExperience = GrowthLevel.nextLevelExperience(experience);
        return new GrowthResponse(
                0,
                experience,
                level.displayCode(),
                level.displayName(),
                rank.displayName(),
                answerRecordMapper.countByUser(user.getId()),
                answerRecordMapper.averageScoreByUser(user.getId()),
                nextLevelExperience,
                Math.max(0, nextLevelExperience - experience),
                growthAwardService.calculateStreakDays(user.getId()),
                growthAwardService.findBadgeWall(user.getId()),
                java.util.Collections.emptyList(),
                growthAwardService.findRecentEvents(user.getId())
        );
    }
}
