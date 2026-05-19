package com.earth.online.player.ailearn.common.util;

import com.earth.online.player.ailearn.common.exception.ClientStreamClosedException;
import java.io.IOException;
import java.util.Locale;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

/**
 * 客户端断开连接识别工具。
 */
public final class ClientDisconnectUtils {

    private static final String CLIENT_ABORT_EXCEPTION_NAME = "ClientAbortException";
    private static final String CONNECTION_ABORTED_MESSAGE = "中止了一个已建立的连接";
    private static final String BROKEN_PIPE_MESSAGE = "broken pipe";
    private static final String CONNECTION_RESET_MESSAGE = "connection reset";

    private ClientDisconnectUtils() {
    }

    /**
     * 判断异常是否由客户端主动断开连接导致。
     *
     * @param exception 待判断异常
     * @return 是否为客户端断开连接
     */
    public static boolean isClientDisconnected(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (isClientDisconnectCause(current)) {
                return true;
            }

            // 继续向下检查根因，兼容 Spring 和 Tomcat 多层包装异常。
            current = current.getCause();
        }
        return false;
    }

    /**
     * 判断单个异常节点是否表示客户端断开。
     *
     * @param exception 当前异常节点
     * @return 是否为客户端断开异常
     */
    private static boolean isClientDisconnectCause(Throwable exception) {
        if (exception instanceof ClientStreamClosedException || exception instanceof AsyncRequestNotUsableException) {
            return true;
        }
        if (exception instanceof IOException && isClientDisconnectMessage(exception.getMessage())) {
            return true;
        }

        // 不直接依赖 Tomcat 类，避免后续更换 Servlet 容器时扩大编译依赖。
        return exception.getClass().getName().endsWith(CLIENT_ABORT_EXCEPTION_NAME);
    }

    /**
     * 根据不同操作系统和容器的异常消息识别客户端断开。
     *
     * @param message 异常消息
     * @return 是否为客户端断开消息
     */
    private static boolean isClientDisconnectMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        // Windows 常见消息为“中止了一个已建立的连接”，Linux 常见消息为 Broken pipe 或 Connection reset。
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        return normalizedMessage.contains(CONNECTION_ABORTED_MESSAGE)
                || normalizedMessage.contains(BROKEN_PIPE_MESSAGE)
                || normalizedMessage.contains(CONNECTION_RESET_MESSAGE);
    }
}
