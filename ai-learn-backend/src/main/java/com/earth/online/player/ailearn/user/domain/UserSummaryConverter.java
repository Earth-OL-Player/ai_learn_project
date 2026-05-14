package com.earth.online.player.ailearn.user.domain;

/**
 * 用户展示转换器。
 */
public final class UserSummaryConverter {

    private static final String DEFAULT_LEVEL = "Lv1";
    private static final String DEFAULT_LEVEL_NAME = "AI 入门者";
    private static final String DEFAULT_RANK = "青铜";

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
        return new UserSummary(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                user.getEmail(),
                user.getExperience(),
                DEFAULT_LEVEL,
                DEFAULT_LEVEL_NAME,
                DEFAULT_RANK,
                user.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime()
        );
    }
}
