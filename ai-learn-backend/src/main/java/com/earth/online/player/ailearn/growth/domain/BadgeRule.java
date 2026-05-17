package com.earth.online.player.ailearn.growth.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * AI 智能刷题勋章规则枚举。
 */
public enum BadgeRule {

    FIRST_ANSWER("FIRST_ANSWER", "ENTRY", "入门类", false, 10),
    ANSWER_10("ANSWER_10", "ENTRY", "入门类", false, 20),
    ANSWER_100("ANSWER_100", "ENTRY", "入门类", false, 30),
    ANSWER_300("ANSWER_300", "ENTRY", "入门类", false, 40),
    LEARNING_3_DAYS("LEARNING_3_DAYS", "PERSISTENCE", "坚持类", false, 50),
    LEARNING_30_DAYS("LEARNING_30_DAYS", "PERSISTENCE", "坚持类", false, 60),
    LEARNING_100_DAYS("LEARNING_100_DAYS", "PERSISTENCE", "坚持类", false, 70),
    LATE_NIGHT("LATE_NIGHT", "RARE", "隐藏/稀有类", true, 80),
    EARLY_MORNING("EARLY_MORNING", "RARE", "隐藏/稀有类", true, 90),
    WEEKEND_PRACTICE("WEEKEND_PRACTICE", "RARE", "隐藏/稀有类", true, 100),
    ASK_TO_END("ASK_TO_END", "RARE", "隐藏/稀有类", true, 110);

    private final String ruleCode;
    private final String category;
    private final String categoryName;
    private final boolean hidden;
    private final int displayOrder;

    /**
     * 创建勋章规则。
     *
     * @param ruleCode 规则编码
     * @param category 分类编码
     * @param categoryName 分类名称
     * @param hidden 是否隐藏勋章
     * @param displayOrder 展示顺序
     */
    BadgeRule(String ruleCode, String category, String categoryName, boolean hidden, int displayOrder) {
        this.ruleCode = ruleCode;
        this.category = category;
        this.categoryName = categoryName;
        this.hidden = hidden;
        this.displayOrder = displayOrder;
    }

    /**
     * 按规则编码查找勋章规则。
     *
     * @param ruleCode 规则编码
     * @return 勋章规则
     */
    public static Optional<BadgeRule> fromRuleCode(String ruleCode) {
        return Arrays.stream(values())
                .filter(rule -> rule.ruleCode.equals(ruleCode))
                .findFirst();
    }

    /**
     * 获取展示顺序。
     *
     * @param ruleCode 规则编码
     * @return 展示顺序
     */
    public static int orderOf(String ruleCode) {
        return fromRuleCode(ruleCode)
                .map(BadgeRule::displayOrder)
                .orElse(Integer.MAX_VALUE);
    }

    /** 获取规则编码。 */
    public String ruleCode() {
        return ruleCode;
    }

    /** 获取分类编码。 */
    public String category() {
        return category;
    }

    /** 获取分类名称。 */
    public String categoryName() {
        return categoryName;
    }

    /** 获取是否隐藏勋章。 */
    public boolean hidden() {
        return hidden;
    }

    /** 获取展示顺序。 */
    public int displayOrder() {
        return displayOrder;
    }
}
