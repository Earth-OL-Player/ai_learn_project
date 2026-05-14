package com.earth.online.player.ailearn.suggestion.domain;

/**
 * 建议类型枚举。
 */
public enum SuggestionType {

    /** 功能建议。 */
    FEATURE("功能建议"),

    /** 体验优化。 */
    EXPERIENCE("体验优化"),

    /** 内容建议。 */
    CONTENT("内容建议"),

    /** 问题反馈。 */
    BUG("问题反馈"),

    /** 其他建议。 */
    OTHER("其他");

    private final String text;

    /**
     * 创建建议类型。
     *
     * @param text 中文文案
     */
    SuggestionType(String text) {
        this.text = text;
    }

    /**
     * 获取中文文案。
     *
     * @return 中文文案
     */
    public String text() {
        return text;
    }
}
