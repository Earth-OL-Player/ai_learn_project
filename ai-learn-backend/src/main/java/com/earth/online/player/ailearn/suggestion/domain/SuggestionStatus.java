package com.earth.online.player.ailearn.suggestion.domain;

/**
 * 建议处理状态枚举。
 */
public enum SuggestionStatus {

    /** 待处理。 */
    PENDING("待处理"),

    /** 已采纳。 */
    ACCEPTED("已采纳"),

    /** 已拒绝。 */
    REJECTED("已拒绝"),

    /** 已完成。 */
    DONE("已完成");

    private final String text;

    /**
     * 创建建议状态。
     *
     * @param text 中文文案
     */
    SuggestionStatus(String text) {
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
