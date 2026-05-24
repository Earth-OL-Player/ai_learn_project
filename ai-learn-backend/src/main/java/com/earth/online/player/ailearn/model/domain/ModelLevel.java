package com.earth.online.player.ailearn.model.domain;

import java.util.Locale;

/**
 * 模型权益等级。
 */
public enum ModelLevel {

    BASIC("初级模型", "deepseek-v4-flash", 0),
    PRO("高级模型", "deepseek-v4-pro", 1),
    SUPER("超级模型", "gpt-5.5", 2);

    private final String label;
    private final String defaultModelName;
    private final int rank;

    /**
     * 创建模型等级。
     *
     * @param label 中文名称
     * @param defaultModelName 默认模型名称
     * @param rank 等级顺序
     */
    ModelLevel(String label, String defaultModelName, int rank) {
        this.label = label;
        this.defaultModelName = defaultModelName;
        this.rank = rank;
    }

    /**
     * 解析模型等级编码。
     *
     * @param value 原始编码
     * @return 模型等级
     */
    public static ModelLevel resolve(String value) {
        if (value == null) {
            return BASIC;
        }
        return ModelLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * 判断当前等级是否高于指定等级。
     *
     * @param other 待比较等级
     * @return 是否更高
     */
    public boolean higherThan(ModelLevel other) {
        return this.rank > other.rank;
    }

    /**
     * 判断当前等级是否不低于指定等级。
     *
     * @param other 待比较等级
     * @return 是否不低于
     */
    public boolean atLeast(ModelLevel other) {
        return this.rank >= other.rank;
    }

    /**
     * 获取中文名称。
     *
     * @return 中文名称
     */
    public String label() {
        return label;
    }

    /**
     * 获取默认模型名称。
     *
     * @return 默认模型名称
     */
    public String defaultModelName() {
        return defaultModelName;
    }
}
