package com.earth.online.player.ailearn.common.exception;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.util.ClientDisconnectUtils;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常。
     *
     * @param exception 业务异常
     * @return 统一失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return buildFailureResponse(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理请求体参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        return buildFailureResponse(ResponseCode.PARAM_INVALID.code(), resolveBindMessage(exception));
    }

    /**
     * 处理绑定参数校验异常。
     *
     * @param exception 绑定异常
     * @return 统一失败响应
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
        return buildFailureResponse(ResponseCode.PARAM_INVALID.code(), resolveBindMessage(exception));
    }

    /**
     * 处理约束参数校验异常。
     *
     * @param exception 约束校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.joining("；"));
        return buildFailureResponse(ResponseCode.PARAM_INVALID.code(), message);
    }

    /**
     * 处理未预期异常。
     *
     * @param exception 未预期异常
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception exception) {
        if (ClientDisconnectUtils.isClientDisconnected(exception)) {
            LOGGER.debug("客户端连接已断开，忽略响应写出异常：{}", exception.getMessage());
            return ResponseEntity.noContent().build();
        }

        // 非客户端断开异常仍按系统异常记录完整堆栈，方便定位真实服务端问题。
        LOGGER.error("系统处理请求时发生异常", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ResponseCode.SYSTEM_ERROR.code(), "系统繁忙，请稍后重试"));
    }

    /**
     * 解析参数绑定错误消息。
     *
     * @param exception 绑定异常
     * @return 可读中文错误消息
     */
    private String resolveBindMessage(BindException exception) {
        return exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? "参数不合法" : error.getDefaultMessage())
                .collect(Collectors.joining("；"));
    }

    /**
     * 构造统一失败响应并写入匹配的 HTTP 状态。
     *
     * @param code 统一业务错误码
     * @param message 错误消息
     * @return 统一失败响应
     */
    private ResponseEntity<ApiResponse<Void>> buildFailureResponse(String code, String message) {
        return ResponseEntity.status(resolveHttpStatus(code)).body(ApiResponse.failure(code, message));
    }

    /**
     * 根据统一业务码解析 HTTP 状态。
     *
     * @param code 统一业务错误码
     * @return HTTP 状态
     */
    private HttpStatus resolveHttpStatus(String code) {
        if (ResponseCode.PARAM_INVALID.code().equals(code) || ResponseCode.BUSINESS_ERROR.code().equals(code)) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ResponseCode.AUTH_UNAUTHORIZED.code().equals(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (ResponseCode.AUTH_FORBIDDEN.code().equals(code)) {
            return HttpStatus.FORBIDDEN;
        }
        if (ResponseCode.RESOURCE_NOT_FOUND.code().equals(code)) {
            return HttpStatus.NOT_FOUND;
        }
        if (ResponseCode.RESOURCE_CONFLICT.code().equals(code)) {
            return HttpStatus.CONFLICT;
        }
        if (ResponseCode.RATE_LIMITED.code().equals(code)) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (ResponseCode.SYSTEM_ERROR.code().equals(code)) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
