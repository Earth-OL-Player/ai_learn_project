package com.earth.online.player.ailearn.user.domain;

import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;

/**
 * 用户展示转换器。
 */
public final class UserSummaryConverter {

    /**
     * 工具类不允许实例化。
     */
    private UserSummaryConverter() {
    }

    /**
     * 将用户领域对象转换为前端安全摘要。
     *
     * @param user 用户领域对象
     * @return 用户摘要
     */
    public static UserSummary toSummary(User user) {
        int experience = user.getExperience() == null ? 0 : user.getExperience();
        GrowthLevel level = GrowthLevel.resolveByExperience(experience);
        GrowthRank rank = GrowthRank.resolveByExperience(experience);
        boolean superAdmin = Boolean.TRUE.equals(user.getSuperAdmin());

        // 用户摘要只返回安全展示字段，不暴露密码哈希等内部信息。
        return new UserSummary(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                user.getEmail(),
                experience,
                level.displayCode(),
                level.displayName(),
                rank.displayName(),
                level.levelValue(),
                level.minExperience(),
                level.nextLevelExperience(),
                Math.max(0, level.nextLevelExperience() - experience),
                level.progressText(experience),
                superAdmin,
                user.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime()
        );
    }
}
