package com.earth.online.player.ailearn.common.ratelimit;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;

/**
 * 限流超限异常。
 */
public class RateLimitExceededException extends BusinessException {

    /**
     * 创建限流超限异常。
     *
     * @param message 错误消息
     */
    public RateLimitExceededException(String message) {
        super(ResponseCode.RATE_LIMITED.code(), message);
    }
}
