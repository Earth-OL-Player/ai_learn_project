package com.earth.online.player.ailearn.common.exception;

import com.earth.online.player.ailearn.common.response.ResponseCode;

/**
 * 业务异常。
 */
public class BusinessException extends RuntimeException {

    private final String code;

    /**
     * 创建业务异常。
     *
     * @param message 错误消息
     */
    public BusinessException(String message) {
        this(ResponseCode.BUSINESS_ERROR.code(), message);
    }

    /**
     * 创建业务异常。
     *
     * @param code 错误编码
     * @param message 错误消息
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取错误编码。
     *
     * @return 错误编码
     */
    public String getCode() {
        return code;
    }
}
