package com.earth.online.player.ailearn.interaction.domain;

import com.earth.online.player.ailearn.common.util.NumberUtils;
import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;

/**
 * 内容作者摘要转换器。
 */
public final class AuthorSummaryConverter {

    /**
     * 工具类不允许实例化。
     */
    private AuthorSummaryConverter() {
    }

    /**
     * 根据作者展示字段构造安全摘要。
     *
     * @param authorId 作者ID
     * @param username 用户名
     * @param nickname 昵称
     * @param avatar 头像地址
     * @param authorExperience 作者经验值
     * @return 作者安全摘要
     */
    public static AuthorSummary toSummary(
            Long authorId,
            String username,
            String nickname,
            String avatar,
            Integer authorExperience) {
        int experience = NumberUtils.toIntOrZero(authorExperience);
        GrowthLevel level = GrowthLevel.resolveByExperience(experience);
        GrowthRank rank = GrowthRank.resolveByExperience(experience);

        // 等级和段位遵循个人中心同一套成长规则。
        return new AuthorSummary(
                String.valueOf(authorId),
                username,
                nickname,
                avatar,
                level.displayCode(),
                level.levelValue(),
                rank.displayName()
        );
    }
}
