package com.earth.online.player.ailearn.practice.domain;

import java.util.Set;

/**
 * AI 智能刷题业务常量。
 */
public final class PracticeConstants {

    /** 等待出题阶段。 */
    public static final String PHASE_QUESTIONING = "QUESTIONING";

    /** 正在答题阶段。 */
    public static final String PHASE_ANSWERING = "ANSWERING";

    /** 本题讨论阶段。 */
    public static final String PHASE_DISCUSSING = "DISCUSSING";

    /** 出题动作。 */
    public static final String ACTION_QUESTION = "QUESTION";

    /** 评分动作。 */
    public static final String ACTION_GRADING = "GRADING";

    /** 讨论动作。 */
    public static final String ACTION_DISCUSSION = "DISCUSSION";

    /** 提示动作。 */
    public static final String ACTION_TIP = "TIP";

    /** 本地讨论兜底提示。 */
    public static final String FALLBACK_DISCUSSION_MESSAGE = "抱歉，当前大模型调用异常，仅保留兜底策略评分功能，无法和您进行探讨。";

    /** 当前题答案最大保存长度。 */
    public static final int MAX_STORED_ANSWER_LENGTH = 4000;

    /** 评分摘要最大保存长度。 */
    public static final int MAX_GRADING_SUMMARY_LENGTH = 2000;

    /** 当前题讨论历史最大消息数。 */
    public static final int MAX_DISCUSSION_HISTORY_MESSAGES = 12;

    /** 讨论历史单条内容最大长度。 */
    public static final int MAX_DISCUSSION_HISTORY_CONTENT_LENGTH = 1000;

    /** 当前轮跨端展示聊天最大消息数。 */
    public static final int MAX_CHAT_HISTORY_MESSAGES = 24;

    /** 当前轮跨端展示聊天单条文本最大长度。 */
    public static final int MAX_CHAT_HISTORY_CONTENT_LENGTH = 4000;

    /** 合格答案最低分。 */
    public static final int PASS_SCORE = 60;

    /** 优秀答案最低分。 */
    public static final int EXCELLENT_SCORE = 80;

    /** 明显偏离刷题上下文的常见词。 */
    public static final Set<String> UNRELATED_WORDS = Set.of("天气", "新闻", "股票", "旅游", "做饭", "写诗", "翻译", "笑话", "帅", "好看", "星座");

    private PracticeConstants() {
    }
}
