package com.earth.online.player.ailearn.growth.domain;

/**
 * 成长等级值对象。
 */
public final class GrowthLevel {

    private static final int MIN_LEVEL = 1;
    private static final int EXPERIENCE_PER_LEVEL = 300;
    private static final String LEVEL_PREFIX = "LV";

    private final int levelValue;
    private final int minExperience;
    private final int nextLevelExperience;

    /**
     * 创建成长等级值对象。
     *
     * @param levelValue 等级数字
     */
    private GrowthLevel(int levelValue) {
        this.levelValue = levelValue;
        this.minExperience = (levelValue - MIN_LEVEL) * EXPERIENCE_PER_LEVEL;
        this.nextLevelExperience = levelValue * EXPERIENCE_PER_LEVEL;
    }

    /**
     * 根据总经验解析等级。
     *
     * @param experience 当前总经验
     * @return 成长等级
     */
    public static GrowthLevel resolveByExperience(int experience) {
        int safeExperience = Math.max(0, experience);
        int resolvedLevel = safeExperience / EXPERIENCE_PER_LEVEL + MIN_LEVEL;
        return new GrowthLevel(resolvedLevel);
    }

    /**
     * 计算下一级所需总经验。
     *
     * @param experience 当前总经验
     * @return 下一级所需总经验
     */
    public static int nextLevelExperience(int experience) {
        return resolveByExperience(experience).nextLevelExperience();
    }

    /**
     * 计算当前等级起始总经验。
     *
     * @param experience 当前总经验
     * @return 当前等级起始总经验
     */
    public static int currentLevelExperience(int experience) {
        return resolveByExperience(experience).minExperience();
    }

    /**
     * 获取数据库编码。
     *
     * @return 数据库编码
     */
    public String code() {
        return displayCode();
    }

    /**
     * 获取等级展示编码。
     *
     * @return 等级展示编码
     */
    public String displayCode() {
        return LEVEL_PREFIX + levelValue;
    }

    /**
     * 获取等级名称。
     *
     * @return 空等级名称
     */
    public String displayName() {
        return "";
    }

    /**
     * 获取等级数字。
     *
     * @return 等级数字
     */
    public int levelValue() {
        return levelValue;
    }

    /**
     * 获取等级最低总经验。
     *
     * @return 当前等级起始总经验
     */
    public int minExperience() {
        return minExperience;
    }

    /**
     * 获取下一级所需总经验。
     *
     * @return 下一级所需总经验
     */
    public int nextLevelExperience() {
        return nextLevelExperience;
    }

    /**
     * 获取等级经验展示文案。
     *
     * @param currentExperience 当前总经验
     * @return 等级经验展示文案
     */
    public String progressText(int currentExperience) {
        return displayCode() + " " + Math.max(0, currentExperience) + "/" + nextLevelExperience;
    }
}
