package com.earth.online.player.ailearn.question.domain;

/**
 * 题目类型枚举。
 */
public enum QuestionType {

    /** 简答题。 */
    SHORT_ANSWER("简答题"),

    /** 选择题。 */
    CHOICE("选择题"),

    /** 编程题。 */
    CODE("编程题"),

    /** 场景题。 */
    SCENARIO("场景题");

    private final String text;

    /**
     * 创建题目类型。
     *
     * @param text 中文文案
     */
    QuestionType(String text) {
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
