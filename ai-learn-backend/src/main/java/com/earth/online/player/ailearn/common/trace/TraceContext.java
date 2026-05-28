package com.earth.online.player.ailearn.common.trace;

import org.slf4j.MDC;

/**
 * 请求链路上下文。
 */
public final class TraceContext {

    /** traceId 请求头名称。 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** traceId 日志 MDC 键名。 */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    /**
     * 工具类不允许实例化。
     */
    private TraceContext() {
    }

    /**
     * 保存当前请求 traceId，并同步写入日志上下文。
     *
     * @param traceId 请求链路标识
     */
    public static void setTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            clear();
            return;
        }

        // ThreadLocal 供业务代码读取，MDC 供日志模板输出。
        String normalizedTraceId = traceId.trim();
        TRACE_ID_HOLDER.set(normalizedTraceId);
        MDC.put(TRACE_ID_MDC_KEY, normalizedTraceId);
    }

    /**
     * 获取当前请求 traceId。
     *
     * @return 请求链路标识
     */
    public static String getTraceId() {
        return TRACE_ID_HOLDER.get();
    }

    /**
     * 清理当前线程上下文和日志上下文，避免线程复用污染。
     */
    public static void clear() {
        TRACE_ID_HOLDER.remove();
        MDC.remove(TRACE_ID_MDC_KEY);
    }
}
