package com.earth.online.player.ailearn.growth.application;

import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;
import org.springframework.stereotype.Service;

/**
 * 成长规则应用服务。
 */
@Service
public class GrowthRuleService {

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
