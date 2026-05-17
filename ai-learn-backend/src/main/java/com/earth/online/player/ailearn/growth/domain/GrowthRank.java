package com.earth.online.player.ailearn.growth.domain;

/**
 * 修仙境界段位枚举。
 */
public enum GrowthRank {

    /** 炼气期，覆盖 LV1 至 LV10。 */
    QI_REFINING("炼气期", 1, 10),

    /** 筑基期，覆盖 LV11 至 LV20。 */
    FOUNDATION_BUILDING("筑基期", 11, 20),

    /** 金丹期，覆盖 LV21 至 LV30。 */
    GOLDEN_CORE("金丹期", 21, 30),

    /** 元婴期，覆盖 LV31 至 LV40。 */
    NASCENT_SOUL("元婴期", 31, 40),

    /** 化神期，覆盖 LV41 至 LV50。 */
    SOUL_FORMATION("化神期", 41, 50),

    /** 炼虚期，覆盖 LV51 至 LV60。 */
    VOID_REFINING("炼虚期", 51, 60),

    /** 合体期，覆盖 LV61 至 LV70。 */
    BODY_INTEGRATION("合体期", 61, 70),

    /** 大乘期，覆盖 LV71 至 LV80。 */
    MAHAYANA("大乘期", 71, 80),

    /** 渡劫期，覆盖 LV81 至 LV90。 */
    TRIBULATION("渡劫期", 81, 90),

    /** 真仙境，覆盖 LV91 至 LV100。 */
    TRUE_IMMORTAL("真仙境", 91, 100),

    /** 金仙境，覆盖 LV101 至 LV110。 */
    GOLDEN_IMMORTAL("金仙境", 101, 110),

    /** 太乙境，覆盖 LV111 至 LV120。 */
    TAIYI("太乙境", 111, 120),

    /** 大罗境，覆盖 LV121 至 LV130。 */
    GREAT_LUO("大罗境", 121, 130),

    /** 道祖境，覆盖 LV131 及以上。 */
    DAO_ANCESTOR("道祖境", 131, Integer.MAX_VALUE);

    private final String displayName;
    private final int minLevel;
    private final int maxLevel;

    /**
     * 创建修仙境界段位。
     *
     * @param displayName 展示名称
     * @param minLevel 最小等级
     * @param maxLevel 最大等级
     */
    GrowthRank(String displayName, int minLevel, int maxLevel) {
        this.displayName = displayName;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    /**
     * 根据总经验解析段位。
     *
     * @param experience 当前总经验
     * @return 修仙境界段位
     */
    public static GrowthRank resolveByExperience(int experience) {
        return resolveByLevel(GrowthLevel.resolveByExperience(experience).levelValue());
    }

    /**
     * 根据等级数字解析段位。
     *
     * @param levelValue 等级数字
     * @return 修仙境界段位
     */
    public static GrowthRank resolveByLevel(int levelValue) {
        int safeLevel = Math.max(1, levelValue);
        for (GrowthRank rank : values()) {
            if (safeLevel >= rank.minLevel && safeLevel <= rank.maxLevel) {
                return rank;
            }
        }
        return DAO_ANCESTOR;
    }

    /**
     * 获取数据库编码。
     *
     * @return 数据库编码
     */
    public String code() {
        return name();
    }

    /**
     * 获取展示名称。
     *
     * @return 展示名称
     */
    public String displayName() {
        return displayName;
    }
}
