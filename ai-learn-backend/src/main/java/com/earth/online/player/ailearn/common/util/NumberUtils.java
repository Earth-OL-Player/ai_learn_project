package com.earth.online.player.ailearn.common.util;

/**
 * 数值规整工具。
 */
public final class NumberUtils {

    private static final int MIN_PERCENT_SCORE = 0;
    private static final int MAX_PERCENT_SCORE = 100;

    /**
     * 工具类不允许实例化。
     */
    private NumberUtils() {
    }

    /**
     * 将可空整数转换为安全值。
     *
     * @param value 原始值
     * @return 非空整数
     */
    public static int toIntOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 将可空整数转换为非负安全值。
     *
     * @param value 原始值
     * @return 非负整数
     */
    public static int toNonNegativeInt(Integer value) {
        return Math.max(0, toIntOrZero(value));
    }

    /**
     * 将百分制分数限制在合法范围内。
     *
     * @param score 原始分数
     * @return 0到100之间的安全分数
     */
    public static int clampPercentScore(int score) {
        return Math.max(MIN_PERCENT_SCORE, Math.min(MAX_PERCENT_SCORE, score));
    }

    /**
     * 将百分制分数归一化为0到1之间的小数。
     *
     * @param score 原始百分制分数
     * @return 归一化分数
     */
    public static double normalizePercentScore(double score) {
        return Math.max(MIN_PERCENT_SCORE, Math.min(MAX_PERCENT_SCORE, score)) / (double) MAX_PERCENT_SCORE;
    }
}
