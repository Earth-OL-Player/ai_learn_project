package com.earth.online.player.ailearn.growth.domain;

/**
 * 成长等级枚举。
 */
public enum GrowthLevel {

    /** AI 入门者。 */
    LV1("Lv1", "AI 入门者", 0),

    /** AI 探索者。 */
    LV2("Lv2", "AI 探索者", 100),

    /** AI 实践者。 */
    LV3("Lv3", "AI 实践者", 300),

    /** AI 进阶者。 */
    LV4("Lv4", "AI 进阶者", 700),

    /** AI 工程师。 */
    LV5("Lv5", "AI 工程师", 1500);

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
     * @return 下一级经验，满级时返回当前等级最低经验
     */
    public static int nextLevelExperience(int experience) {
        GrowthLevel current = resolveByExperience(experience);
        GrowthLevel[] levels = values();
        for (int index = 0; index < levels.length; index++) {
            if (levels[index] == current && index + 1 < levels.length) {
                return levels[index + 1].minExperience;
            }
        }
        return current.minExperience;
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
}
