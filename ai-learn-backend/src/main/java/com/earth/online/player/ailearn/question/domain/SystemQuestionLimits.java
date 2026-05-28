package com.earth.online.player.ailearn.question.domain;

/**
 * 系统题库业务边界常量。
 */
public final class SystemQuestionLimits {

    /** 单次导入题目上限。 */
    public static final int MAX_IMPORT_ROWS = 1000;

    /** 单次导入文件大小上限。 */
    public static final long MAX_IMPORT_FILE_SIZE = 2 * 1024 * 1024L;

    /** 题目编码最大长度。 */
    public static final int MAX_CODE_LENGTH = 64;

    /** 题目分类最大长度。 */
    public static final int MAX_QUESTION_TYPE_LENGTH = 32;

    /** 题目和参考答案最大长度。 */
    public static final int MAX_LONG_TEXT_LENGTH = 10000;

    /** 题目编码长度错误提示。 */
    public static final String CODE_TOO_LONG_MESSAGE = "题目编码不能超过" + MAX_CODE_LENGTH + "个字符";

    /** 题目长度错误提示。 */
    public static final String QUESTION_TOO_LONG_MESSAGE = "题目不能超过" + MAX_LONG_TEXT_LENGTH + "个字符";

    /** 题目分类长度错误提示。 */
    public static final String QUESTION_TYPE_TOO_LONG_MESSAGE = "题目分类不能超过" + MAX_QUESTION_TYPE_LENGTH + "个字符";

    /** 参考答案长度错误提示。 */
    public static final String STANDARD_ANSWER_TOO_LONG_MESSAGE = "参考答案不能超过" + MAX_LONG_TEXT_LENGTH + "个字符";

    /** 单次导入行数错误提示。 */
    public static final String IMPORT_ROWS_TOO_MANY_MESSAGE = "单次最多导入" + MAX_IMPORT_ROWS + "道题";

    /** CSV文件大小错误提示。 */
    public static final String IMPORT_FILE_TOO_LARGE_MESSAGE = "CSV文件不能超过2MB";

    /**
     * 隐藏业务常量类构造方法。
     */
    private SystemQuestionLimits() {
    }
}
