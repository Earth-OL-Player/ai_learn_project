package com.earth.online.player.ailearn.practice.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.practice.application.PracticeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 智能刷题接口。
 */
@RestController
@RequestMapping("/api/v1/practice")
public class PracticeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PracticeController.class);
    private static final int SSE_TIMEOUT_MILLIS = 120000;
    private static final int STREAM_CHUNK_SIZE = 8;
    private static final int STREAM_CHUNK_INTERVAL_MILLIS = 20;

    private final PracticeService practiceService;
    private final ObjectMapper objectMapper;

    /**
     * 创建 AI 智能刷题接口。
     *
     * @param practiceService 刷题应用服务
     * @param objectMapper JSON 序列化器
     */
    public PracticeController(PracticeService practiceService, ObjectMapper objectMapper) {
        this.practiceService = practiceService;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询题目分类。
     *
     * @return 题目分类
     */
    @GetMapping("/categories")
    public ApiResponse<List<String>> findQuestionTypes() {
        return ApiResponse.success(practiceService.findQuestionTypes());
    }

    /**
     * 查询当前刷题状态。
     *
     * @return 当前状态
     */
    @GetMapping("/state")
    public ApiResponse<PracticeStateResponse> getState() {
        return ApiResponse.success(practiceService.getState());
    }

    /**
     * 抽取下一题。
     *
     * @param request 出题请求
     * @return 出题结果
     */
    @PostMapping("/next-question")
    public ApiResponse<PracticeMessageResponse> nextQuestion(@RequestBody(required = false) PracticeActionRequest request) {
        return ApiResponse.success(practiceService.nextQuestion(request));
    }

    /**
     * 重新回答当前题。
     *
     * @return 当前题
     */
    @PostMapping("/retry")
    public ApiResponse<PracticeMessageResponse> retryCurrentQuestion() {
        return ApiResponse.success(practiceService.retryCurrentQuestion());
    }

    /**
     * 处理用户聊天输入。
     *
     * @param request 聊天请求
     * @return 聊天结果
     */
    @PostMapping("/messages")
    public ApiResponse<PracticeMessageResponse> handleMessage(@RequestBody PracticeMessageRequest request) {
        return ApiResponse.success(practiceService.handleMessage(request));
    }

    /**
     * 以 SSE 流式处理用户聊天输入。
     *
     * @param request 聊天请求
     * @return SSE 发射器
     */
    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleMessageStream(@RequestBody PracticeMessageRequest request) {
        SseEmitter emitter = new SseEmitter((long) SSE_TIMEOUT_MILLIS);
        AuthenticatedUser authenticatedUser = AuthContext.getUser();

        // 异步线程中恢复认证上下文，保证复用原有业务服务和权限校验。
        CompletableFuture.runAsync(() -> emitMessageStream(request, emitter, authenticatedUser));
        return emitter;
    }

    /**
     * 输出聊天流事件。
     *
     * @param request 聊天请求
     * @param emitter SSE 发射器
     * @param authenticatedUser 当前用户
     */
    private void emitMessageStream(PracticeMessageRequest request, SseEmitter emitter, AuthenticatedUser authenticatedUser) {
        AtomicBoolean chunkSent = new AtomicBoolean(false);
        try {
            AuthContext.setUser(authenticatedUser);
            PracticeMessageResponse result = practiceService.handleMessageStream(request, chunk -> {
                emitChunk(emitter, chunk);
                chunkSent.set(true);
            });
            if (!chunkSent.get()) {
                emitTextChunks(emitter, result.message());
            }
            emitter.send(SseEmitter.event().name("result").data(toJson(result)));
            emitter.complete();
        } catch (IOException | RuntimeException exception) {
            emitError(emitter, exception.getMessage());
        } finally {
            AuthContext.clear();
        }
    }

    /**
     * 分片输出回复文本。
     *
     * @param emitter SSE 发射器
     * @param message 回复文本
     * @throws IOException SSE 输出异常
     */
    private void emitTextChunks(SseEmitter emitter, String message) throws IOException {
        String safeMessage = message == null ? "" : message;
        for (int index = 0; index < safeMessage.length(); index += STREAM_CHUNK_SIZE) {
            int endIndex = Math.min(index + STREAM_CHUNK_SIZE, safeMessage.length());
            emitter.send(SseEmitter.event().name("message").data(safeMessage.substring(index, endIndex)));
            if (!pauseBetweenChunks()) {
                break;
            }
        }
    }

    /**
     * 输出真实模型返回的流式片段。
     *
     * @param emitter SSE 发射器
     * @param chunk 文本片段
     */
    private void emitChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event().name("message").data(chunk));
        } catch (IOException exception) {
            throw new IllegalStateException("流式消息发送失败", exception);
        }
    }

    /**
     * 控制前端可感知的流式输出节奏。
     *
     * @return 是否继续发送
     */
    private boolean pauseBetweenChunks() {
        try {
            Thread.sleep(STREAM_CHUNK_INTERVAL_MILLIS);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 输出流式错误事件。
     *
     * @param emitter SSE 发射器
     * @param message 错误消息
     */
    private void emitError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message == null ? "请求处理失败" : message));
        } catch (IOException exception) {
            LOGGER.debug("SSE 错误事件发送失败，客户端可能已断开", exception);
        } finally {
            emitter.complete();
        }
    }

    /**
     * 序列化响应对象。
     *
     * @param result 响应对象
     * @return JSON 字符串
     * @throws JsonProcessingException JSON 序列化异常
     */
    private String toJson(PracticeMessageResponse result) throws JsonProcessingException {
        return objectMapper.writeValueAsString(result);
    }
}
