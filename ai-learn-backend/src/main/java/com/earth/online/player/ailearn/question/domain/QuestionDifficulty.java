package com.earth.online.player.ailearn.question.domain;

/**
 * 题目难度枚举。
 */
public enum QuestionDifficulty {

    /** 简单。 */
    EASY("简单"),

    /** 中等。 */
    MEDIUM("中等"),

    /** 困难。 */
    HARD("困难");

    private final String text;

    /**
     * 创建题目难度。
     *
     * @param text 中文文案
     */
    QuestionDifficulty(String text) {
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
