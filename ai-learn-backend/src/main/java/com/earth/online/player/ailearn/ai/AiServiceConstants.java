package com.earth.online.player.ailearn.ai;

/**
 * AI 服务内部调用常量。
 */
public final class AiServiceConstants {

    /** 内部调用鉴权请求头。 */
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    /** JSON 请求和响应内容类型。 */
    public static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    /** SSE 响应内容类型。 */
    public static final String EVENT_STREAM_CONTENT_TYPE = "text/event-stream";

    /** 统一成功响应码。 */
    public static final String SUCCESS_CODE = "SUCCESS";

    /** AI 服务答案评分接口。 */
    public static final String PRACTICE_GRADE_PATH = "/internal/v1/practice/answer/grade";

    /** AI 服务本题流式讨论接口。 */
    public static final String PRACTICE_DISCUSS_STREAM_PATH = "/internal/v1/practice/discuss/stream";

    /** AI 服务日志级别管理接口。 */
    public static final String LOG_LEVEL_PATH = "/internal/v1/log-levels";

    private AiServiceConstants() {
    }
}
