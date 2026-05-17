package com.earth.online.player.ailearn.interaction.domain;

/**
 * 互动纯文字内容规则。
 */
public final class InteractionTextPolicy {

    /** 内容最小长度。 */
    public static final int MIN_CONTENT_LENGTH = 2;

    /** 内容最大长度。 */
    public static final int MAX_CONTENT_LENGTH = 1000;

    private static final char HALF_WIDTH_AT = '@';
    private static final char FULL_WIDTH_AT = '＠';
    private static final int VARIATION_SELECTOR_START = 0xFE00;
    private static final int VARIATION_SELECTOR_END = 0xFE0F;
    private static final int ZERO_WIDTH_JOINER = 0x200D;
    private static final int MISC_SYMBOL_START = 0x2600;
    private static final int MISC_SYMBOL_END = 0x27BF;
    private static final int EMOJI_START = 0x1F000;
    private static final int EMOJI_END = 0x1FAFF;

    /**
     * 工具类不允许实例化。
     */
    private InteractionTextPolicy() {
    }

    /**
     * 规整用户输入内容。
     *
     * @param content 原始内容
     * @return 去除首尾空格后的内容
     */
    public static String normalize(String content) {
        if (content == null) {
            return "";
        }
        return content.trim();
    }

    /**
     * 判断内容长度是否不合法。
     *
     * @param content 已规整内容
     * @return 是否长度不合法
     */
    public static boolean hasInvalidLength(String content) {
        int length = content.length();
        return length < MIN_CONTENT_LENGTH || length > MAX_CONTENT_LENGTH;
    }

    /**
     * 判断内容是否包含艾特符号。
     *
     * @param content 已规整内容
     * @return 是否包含艾特符号
     */
    public static boolean containsMentionMark(String content) {
        return content.indexOf(HALF_WIDTH_AT) >= 0 || content.indexOf(FULL_WIDTH_AT) >= 0;
    }

    /**
     * 判断内容是否包含表情字符。
     *
     * @param content 已规整内容
     * @return 是否包含表情字符
     */
    public static boolean containsEmoji(String content) {
        return content.codePoints().anyMatch(InteractionTextPolicy::isEmojiCodePoint);
    }

    /**
     * 判断内容是否包含本迭代禁止的互动符号。
     *
     * @param content 已规整内容
     * @return 是否包含禁止内容
     */
    public static boolean containsUnsupportedContent(String content) {
        return containsMentionMark(content) || containsEmoji(content);
    }

    /**
     * 判断码点是否属于常见表情范围。
     *
     * @param codePoint Unicode 码点
     * @return 是否为表情相关码点
     */
    private static boolean isEmojiCodePoint(int codePoint) {
        if (codePoint == ZERO_WIDTH_JOINER) {
            return true;
        }
        if (codePoint >= VARIATION_SELECTOR_START && codePoint <= VARIATION_SELECTOR_END) {
            return true;
        }
        if (codePoint >= MISC_SYMBOL_START && codePoint <= MISC_SYMBOL_END) {
            return true;
        }
        return codePoint >= EMOJI_START && codePoint <= EMOJI_END;
    }
}
