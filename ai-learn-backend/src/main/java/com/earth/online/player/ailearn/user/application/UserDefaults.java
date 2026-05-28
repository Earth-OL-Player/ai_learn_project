package com.earth.online.player.ailearn.user.application;

import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;
import com.earth.online.player.ailearn.user.domain.User;

/**
 * 用户默认字段初始化器。
 */
public final class UserDefaults {

    /** 新用户默认经验值。 */
    public static final int DEFAULT_EXPERIENCE = 0;

    /**
     * 工具类不允许实例化。
     */
    private UserDefaults() {
    }

    /**
     * 初始化新用户成长字段。
     *
     * @param user 用户对象
     */
    public static void applyNewUserGrowth(User user) {
        user.setExperience(DEFAULT_EXPERIENCE);
        user.setLevelCode(GrowthLevel.resolveByExperience(DEFAULT_EXPERIENCE).code());
        user.setRankCode(GrowthRank.resolveByExperience(DEFAULT_EXPERIENCE).code());
    }
}
