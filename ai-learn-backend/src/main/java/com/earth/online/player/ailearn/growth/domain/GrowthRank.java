package com.earth.online.player.ailearn.growth.domain;

/**
 * 成长段位枚举。
 */
public enum GrowthRank {

    /** 青铜段位。 */
    BRONZE("青铜", 0),

    /** 白银段位。 */
    SILVER("白银", 300),

    /** 黄金段位。 */
    GOLD("黄金", 700),

    /** 铂金段位。 */
    PLATINUM("铂金", 1500),

    /** 钻石段位。 */
    DIAMOND("钻石", 3000);

    private final String displayName;
    private final int minExperience;

    /**
     * 创建成长段位。
     *
     * @param displayName 展示名称
     * @param minExperience 最低经验值
     */
    GrowthRank(String displayName, int minExperience) {
        this.displayName = displayName;
        this.minExperience = minExperience;
    }

    /**
     * 根据经验解析段位。
     *
     * @param experience 当前经验
     * @return 成长段位
     */
    public static GrowthRank resolveByExperience(int experience) {
        GrowthRank matched = BRONZE;
        for (GrowthRank rank : values()) {
            if (experience >= rank.minExperience) {
                matched = rank;
            }
        }
        return matched;
    }

    /** 获取数据库编码。 */
    public String code() {
        return name();
    }

    /** 获取展示名称。 */
    public String displayName() {
        return displayName;
    }
}
