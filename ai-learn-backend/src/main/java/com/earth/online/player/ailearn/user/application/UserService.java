package com.earth.online.player.ailearn.user.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;
import com.earth.online.player.ailearn.growth.infrastructure.GrowthMapper;
import com.earth.online.player.ailearn.user.domain.User;
import com.earth.online.player.ailearn.user.domain.UserSummary;
import com.earth.online.player.ailearn.user.domain.UserSummaryConverter;
import com.earth.online.player.ailearn.user.infrastructure.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 用户应用服务。
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final GrowthMapper growthMapper;

    /**
     * 创建用户应用服务。
     *
     * @param userMapper 用户仓储
     * @param growthMapper 成长仓储
     */
    public UserService(UserMapper userMapper, GrowthMapper growthMapper) {
        this.userMapper = userMapper;
        this.growthMapper = growthMapper;
    }

    /**
     * 查询当前登录用户摘要。
     *
     * @return 当前用户摘要
     */
    public UserSummary getCurrentUser() {
        AuthenticatedUser authenticatedUser = AuthContext.getUser();
        if (authenticatedUser == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录后即可使用该功能");
        }

        // 每次读取数据库，保证前端拿到最新用户展示信息。
        User user = userMapper.findById(authenticatedUser.userId());
        if (user == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录状态已失效，请重新登录");
        }

        // 成长摘要始终基于所有题目历史最高分总和，避免用户表旧字段造成展示偏差。
        int experience = Math.max(0, growthMapper.sumBestScores(user.getId()));
        GrowthLevel level = GrowthLevel.resolveByExperience(experience);
        GrowthRank rank = GrowthRank.resolveByExperience(experience);
        userMapper.updateGrowth(user.getId(), experience, level.code(), rank.code());
        user.setExperience(experience);
        user.setLevelCode(level.code());
        user.setRankCode(rank.code());
        return UserSummaryConverter.toSummary(user);
    }
}
