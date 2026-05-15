package com.earth.online.player.ailearn.growth.application;

import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;
import org.springframework.stereotype.Service;

/**
 * 成长规则应用服务。
 */
@Service
public class GrowthRuleService {

    private static final int BASE_EXPERIENCE = 5;
    private static final int SCORE_STEP = 20;
    private static final int STEP_EXPERIENCE = 2;

    /**
     * 根据得分计算本次获得经验。
     *
     * @param score 答题得分
     * @return 获得经验
     */
    public int calculateEarnedExperience(int score) {
        int safeScore = Math.max(0, Math.min(100, score));
        return BASE_EXPERIENCE + (safeScore / SCORE_STEP) * STEP_EXPERIENCE;
    }

    /**
     * 根据经验解析等级。
     *
     * @param experience 当前经验
     * @return 等级
     */
    public GrowthLevel resolveLevel(int experience) {
        return GrowthLevel.resolveByExperience(experience);
    }

    /**
     * 根据经验解析段位。
     *
     * @param experience 当前经验
     * @return 段位
     */
    public GrowthRank resolveRank(int experience) {
        return GrowthRank.resolveByExperience(experience);
    }
}
