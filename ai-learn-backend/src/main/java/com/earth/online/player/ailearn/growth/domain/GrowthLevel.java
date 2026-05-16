package com.earth.online.player.ailearn.growth.domain;

/**
 * 成长等级枚举。
 */
public enum GrowthLevel {

    /** Lv1 初识 AI。 */
    LV1("Lv1", "AI 初识者", 0),

    /** Lv2 基础筑基。 */
    LV2("Lv2", "AI 筑基者", 3000),

    /** Lv3 持续练习。 */
    LV3("Lv3", "AI 练习生", 6000),

    /** Lv4 能力进阶。 */
    LV4("Lv4", "AI 进阶者", 9000),

    /** Lv5 项目实践。 */
    LV5("Lv5", "AI 实践者", 12000),

    /** Lv6 工程熟手。 */
    LV6("Lv6", "AI 工程师", 15000),

    /** Lv7 架构视角。 */
    LV7("Lv7", "AI 架构师", 18000),

    /** Lv8 深度掌握。 */
    LV8("Lv8", "AI 高阶专家", 21000),

    /** Lv9 全栈融会。 */
    LV9("Lv9", "AI 全栈专家", 24000),

    /** Lv10 满级大师。 */
    LV10("Lv10", "AI 闯关大师", 27000);

    private final String displayCode;
    private final String displayName;
    private final int minExperience;

    /**
     * 创建成长等级。
     *
     * @param displayCode 展示编码
     * @param displayName 展示名称
     * @param minExperience 最低经验值
     */
    GrowthLevel(String displayCode, String displayName, int minExperience) {
        this.displayCode = displayCode;
        this.displayName = displayName;
        this.minExperience = minExperience;
    }

    /**
     * 根据经验解析等级。
     *
     * @param experience 当前经验
     * @return 成长等级
     */
    public static GrowthLevel resolveByExperience(int experience) {
        GrowthLevel matched = LV1;
        for (GrowthLevel level : values()) {
            if (experience >= level.minExperience) {
                matched = level;
            }
        }
        return matched;
    }

    /**
     * 计算下一级所需经验。
     *
     * @param experience 当前经验
     * @return 下一级经验，满级时返回当前经验
     */
    public static int nextLevelExperience(int experience) {
        GrowthLevel current = resolveByExperience(experience);
        GrowthLevel[] levels = values();
        for (int index = 0; index < levels.length; index++) {
            if (levels[index] == current && index + 1 < levels.length) {
                return levels[index + 1].minExperience;
            }
        }
        return Math.max(experience, current.minExperience);
    }

    /** 获取数据库编码。 */
    public String code() {
        return name();
    }

    /** 获取展示编码。 */
    public String displayCode() {
        return displayCode;
    }

    /** 获取展示名称。 */
    public String displayName() {
        return displayName;
    }

    /** 获取等级最低经验。 */
    public int minExperience() {
        return minExperience;
    }
}
