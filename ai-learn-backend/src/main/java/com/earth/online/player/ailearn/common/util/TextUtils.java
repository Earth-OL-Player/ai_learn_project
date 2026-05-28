package com.earth.online.player.ailearn.common.util;

import org.springframework.util.StringUtils;

/**
 * 文本规整工具。
 */
public final class TextUtils {

    private static final String TRUNCATED_SUFFIX = "……";

    /**
     * 工具类不允许实例化。
     */
    private TextUtils() {
    }

    /**
     * 去除首尾空白，空文本统一转为 null。
     *
     * @param value 原始文本
     * @return 规整后的文本
     */
    public static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 去除首尾空白，空文本统一转为空字符串。
     *
     * @param value 原始文本
     * @return 规整后的文本
     */
    public static String trimToEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    /**
     * 限制文本长度，超长时追加截断提示。
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    public static String limitText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        // 统一使用可读的截断提示，避免各业务重复维护省略符号。
        return text.substring(0, maxLength) + TRUNCATED_SUFFIX;
    }
}
