package com.earth.online.player.ailearn.common.response;

import com.earth.online.player.ailearn.common.trace.TraceContext;

/**
 * 后端统一响应对象。
 *
 * @param code 响应编码
 * @param message 响应消息
 * @param data 业务数据
 * @param traceId 请求链路标识
 * @param <T> 业务数据类型
 */
public record ApiResponse<T>(String code, String message, T data, String traceId) {

    /**
     * 构造成功响应。
     *
     * @param data 业务数据
     * @param <T> 业务数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseCode.SUCCESS.code(), "操作成功", data, TraceContext.getTraceId());
    }

    /**
     * 构造失败响应。
     *
     * @param code 错误编码
     * @param message 错误消息
     * @param <T> 业务数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(code, message, null, TraceContext.getTraceId());
    }
}
