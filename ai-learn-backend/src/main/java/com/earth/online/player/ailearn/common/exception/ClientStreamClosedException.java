package com.earth.online.player.ailearn.common.exception;

/**
 * 客户端流式连接关闭异常。
 */
public class ClientStreamClosedException extends RuntimeException {

    /**
     * 创建客户端流式连接关闭异常。
     *
     * @param message 异常说明
     * @param cause 原始异常
     */
    public ClientStreamClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
