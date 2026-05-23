package com.earth.online.player.ailearn.practice.interfaces;

import com.earth.online.player.ailearn.common.config.AiStreamExecutorConfig;
import com.earth.online.player.ailearn.common.exception.ClientStreamClosedException;
import com.earth.online.player.ailearn.common.ratelimit.InMemoryRateLimitService;
import com.earth.online.player.ailearn.common.ratelimit.RateLimitIdentityResolver;
import com.earth.online.player.ailearn.common.ratelimit.RateLimitLease;
import com.earth.online.player.ailearn.common.ratelimit.RateLimitProperties;
import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.common.trace.TraceContext;
import com.earth.online.player.ailearn.common.util.ClientDisconnectUtils;
import com.earth.online.player.ailearn.practice.application.PracticeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
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
    private static final String AI_CONCURRENT_RULE = "practice-ai-concurrent";

    private final PracticeService practiceService;
    private final ObjectMapper objectMapper;
    private final ThreadPoolTaskExecutor aiStreamTaskExecutor;
    private final RateLimitProperties rateLimitProperties;
    private final InMemoryRateLimitService rateLimitService;
    private final RateLimitIdentityResolver identityResolver;

    /**
     * 创建 AI 智能刷题接口。
     *
     * @param practiceService 刷题应用服务
     * @param objectMapper JSON 序列化器
     * @param aiStreamTaskExecutor AI 流式请求线程池
     * @param rateLimitProperties 限流配置
     * @param rateLimitService 内存限流服务
     * @param identityResolver 限流身份解析器
     */
    public PracticeController(
            PracticeService practiceService,
            ObjectMapper objectMapper,
            @Qualifier(AiStreamExecutorConfig.AI_STREAM_EXECUTOR_BEAN_NAME) ThreadPoolTaskExecutor aiStreamTaskExecutor,
            RateLimitProperties rateLimitProperties,
            InMemoryRateLimitService rateLimitService,
            RateLimitIdentityResolver identityResolver) {
        this.practiceService = practiceService;
        this.objectMapper = objectMapper;
        this.aiStreamTaskExecutor = aiStreamTaskExecutor;
        this.rateLimitProperties = rateLimitProperties;
        this.rateLimitService = rateLimitService;
        this.identityResolver = identityResolver;
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
     * 以 SSE 流式处理用户聊天输入。
     *
     * @param request 聊天请求
     * @return SSE 发射器
     */
    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> handleMessageStream(@RequestBody PracticeMessageRequest request) {
        AuthenticatedUser authenticatedUser = AuthContext.getUser();
        RateLimitLease aiLease = acquireAiConcurrency(authenticatedUser);
        SseEmitter emitter = new SseEmitter((long) SSE_TIMEOUT_MILLIS);
        String traceId = TraceContext.getTraceId();
        AtomicBoolean streamClosed = new AtomicBoolean(false);
        AtomicReference<Future<?>> streamTaskReference = new AtomicReference<>();

        // 先注册生命周期，保证客户端断开时可以取消已经提交的 AI 流式任务。
        registerEmitterLifecycle(emitter, streamClosed, streamTaskReference, aiLease);
        try {
            Future<?> streamTask = aiStreamTaskExecutor.submit(
                    () -> emitMessageStream(request, emitter, authenticatedUser, traceId, streamClosed, aiLease)
            );
            streamTaskReference.set(streamTask);
            if (streamClosed.get()) {
                streamTask.cancel(true);
                aiLease.close();
            }
        } catch (RejectedExecutionException exception) {
            aiLease.close();
            streamClosed.set(true);
            LOGGER.warn("AI 流式请求线程池已满，拒绝本次 SSE 请求", exception);
            emitError(emitter, "当前 AI 流式请求较多，请稍后再试");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }

    /**
     * 输出聊天流事件。
     *
     * @param request 聊天请求
     * @param emitter SSE 发射器
     * @param authenticatedUser 当前用户
     * @param traceId 请求链路标识
     * @param streamClosed 流关闭标记
     * @param aiLease AI 并发占用租约
     */
    private void emitMessageStream(
            PracticeMessageRequest request,
            SseEmitter emitter,
            AuthenticatedUser authenticatedUser,
            String traceId,
            AtomicBoolean streamClosed,
            RateLimitLease aiLease) {
        AtomicBoolean chunkSent = new AtomicBoolean(false);
        AtomicInteger chunkCount = new AtomicInteger(0);
        long startMillis = System.currentTimeMillis();
        try {
            AuthContext.setUser(authenticatedUser);
            TraceContext.setTraceId(traceId);
            sendEvent(emitter, SseEmitter.event().comment("stream-open"), streamClosed);
            PracticeMessageResponse result = practiceService.handleMessageStream(request, chunk -> {
                emitChunk(emitter, chunk, chunkCount.incrementAndGet(), startMillis, streamClosed);
                chunkSent.set(true);
            });
            if (!chunkSent.get()) {
                emitTextChunks(emitter, result.message(), streamClosed);
            }
            sendEvent(emitter, SseEmitter.event().name("result").data(toJson(result)), streamClosed);
            streamClosed.set(true);
            emitter.complete();
        } catch (ClientStreamClosedException exception) {
            LOGGER.info("SSE 客户端连接已断开，停止本次流式输出：{}", exception.getMessage());
            completeSilently(emitter);
        } catch (IOException | RuntimeException exception) {
            emitError(emitter, exception.getMessage());
        } finally {
            aiLease.close();
            AuthContext.clear();
            TraceContext.clear();
        }
    }

    /**
     * 获取单用户 AI 并发限流租约。
     *
     * @param authenticatedUser 当前用户
     * @return AI 并发租约
     */
    private RateLimitLease acquireAiConcurrency(AuthenticatedUser authenticatedUser) {
        if (!rateLimitProperties.isEnabled()) {
            return skippedAiConcurrencyLease();
        }
        String userKey = identityResolver.resolveUserKey(authenticatedUser);
        if (!StringUtils.hasText(userKey)) {
            return skippedAiConcurrencyLease();
        }

        // AI 评分和讨论都走同一个流式入口，统一限制单用户并发请求数量。
        return rateLimitService.acquireConcurrency(AI_CONCURRENT_RULE, userKey, rateLimitProperties.getAiConcurrentLimit());
    }

    /**
     * 创建未占用并发额度的空租约。
     *
     * @return 空租约
     */
    private RateLimitLease skippedAiConcurrencyLease() {
        return new RateLimitLease(() -> LOGGER.trace("AI 并发限流未占用，无需释放"));
    }

    /**
     * 注册 SSE 生命周期回调。
     *
     * @param emitter SSE 发射器
     * @param streamClosed 流关闭标记
     * @param streamTaskReference 流式任务引用
     * @param aiLease AI 并发占用租约
     */
    private void registerEmitterLifecycle(
            SseEmitter emitter,
            AtomicBoolean streamClosed,
            AtomicReference<Future<?>> streamTaskReference,
            RateLimitLease aiLease) {
        emitter.onCompletion(() -> closeStream(streamClosed, streamTaskReference, aiLease));
        emitter.onTimeout(() -> {
            closeStream(streamClosed, streamTaskReference, aiLease);
            LOGGER.debug("SSE 连接超时，已标记流式输出结束");
        });

        // 客户端关闭浏览器时也会进入错误回调，此时不打印 ERROR 堆栈。
        emitter.onError(exception -> handleEmitterError(exception, streamClosed, streamTaskReference, aiLease));
    }

    /**
     * 标记流式连接关闭并取消异步任务。
     *
     * @param streamClosed 流关闭标记
     * @param streamTaskReference 流式任务引用
     * @param aiLease AI 并发占用租约
     */
    private void closeStream(
            AtomicBoolean streamClosed,
            AtomicReference<Future<?>> streamTaskReference,
            RateLimitLease aiLease) {
        if (!streamClosed.compareAndSet(false, true)) {
            return;
        }
        Future<?> streamTask = streamTaskReference.get();
        if (streamTask != null && !streamTask.isDone()) {
            streamTask.cancel(true);
        }
        aiLease.close();
    }

    /**
     * 处理 SSE 生命周期错误。
     *
     * @param exception 生命周期异常
     * @param streamClosed 流关闭标记
     * @param streamTaskReference 流式任务引用
     * @param aiLease AI 并发占用租约
     */
    private void handleEmitterError(
            Throwable exception,
            AtomicBoolean streamClosed,
            AtomicReference<Future<?>> streamTaskReference,
            RateLimitLease aiLease) {
        closeStream(streamClosed, streamTaskReference, aiLease);
        if (ClientDisconnectUtils.isClientDisconnected(exception)) {
            LOGGER.debug("SSE 客户端连接已断开，生命周期错误已忽略：{}", exception.getMessage());
            return;
        }

        // 非客户端断开错误仍保留告警，便于发现服务端异常。
        LOGGER.warn("SSE 连接发生异常，已标记流式输出结束", exception);
    }

    /**
     * 分片输出回复文本。
     *
     * @param emitter SSE 发射器
     * @param message 回复文本
     * @param streamClosed 流关闭标记
     * @throws IOException SSE 输出异常
     */
    private void emitTextChunks(SseEmitter emitter, String message, AtomicBoolean streamClosed) throws IOException {
        String safeMessage = message == null ? "" : message;
        for (int index = 0; index < safeMessage.length(); index += STREAM_CHUNK_SIZE) {
            int endIndex = Math.min(index + STREAM_CHUNK_SIZE, safeMessage.length());
            sendEvent(emitter, SseEmitter.event().name("message").data(safeMessage.substring(index, endIndex)), streamClosed);
        }
    }

    /**
     * 输出真实模型返回的流式片段。
     *
     * @param emitter SSE 发射器
     * @param chunk 文本片段
     * @param chunkCount 已发送片段数
     * @param startMillis 开始时间
     * @param streamClosed 流关闭标记
     */
    private void emitChunk(SseEmitter emitter, String chunk, int chunkCount, long startMillis, AtomicBoolean streamClosed) {
        try {
            sendEvent(emitter, SseEmitter.event().name("message").data(chunk), streamClosed);
            logEmittedChunk(chunk, chunkCount, startMillis);
        } catch (IOException exception) {
            throw new IllegalStateException("流式消息发送失败", exception);
        }
    }

    /**
     * 安全发送 SSE 事件。
     *
     * @param emitter SSE 发射器
     * @param event SSE 事件
     * @param streamClosed 流关闭标记
     * @throws IOException 非客户端断开的发送异常
     */
    private void sendEvent(SseEmitter emitter, SseEmitter.SseEventBuilder event, AtomicBoolean streamClosed) throws IOException {
        if (streamClosed.get()) {
            throw new ClientStreamClosedException("客户端连接已关闭", null);
        }
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            handleSendFailure(exception, streamClosed);
        }
    }

    /**
     * 处理 SSE 发送失败。
     *
     * @param exception 发送异常
     * @param streamClosed 流关闭标记
     * @throws IOException 非客户端断开的 IO 异常
     */
    private void handleSendFailure(Exception exception, AtomicBoolean streamClosed) throws IOException {
        if (ClientDisconnectUtils.isClientDisconnected(exception)) {
            streamClosed.set(true);
            throw new ClientStreamClosedException("客户端连接已关闭", exception);
        }
        if (exception instanceof IOException ioException) {
            throw ioException;
        }

        // 非客户端断开的非法状态继续向上抛出，交由原有异常流程处理。
        if (exception instanceof IllegalStateException illegalStateException) {
            throw illegalStateException;
        }

        // 理论上不会进入该分支，兜底保留原始异常链路。
        throw new IllegalStateException("SSE 发送失败", exception);
    }

    /**
     * 记录后端 SSE 片段输出情况。
     *
     * @param chunk 文本片段
     * @param chunkCount 片段数量
     * @param startMillis 开始时间
     */
    private void logEmittedChunk(String chunk, int chunkCount, long startMillis) {
        if (chunkCount != 1 && chunkCount % 50 != 0) {
            return;
        }

        // 只记录长度和耗时，不记录用户答案或模型文本正文。
        LOGGER.info(
                "Java 后端已发送 SSE 流式片段：count={} chars={} elapsedMs={}",
                chunkCount,
                chunk == null ? 0 : chunk.length(),
                System.currentTimeMillis() - startMillis
        );
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
     * 静默完成 SSE 连接。
     *
     * @param emitter SSE 发射器
     */
    private void completeSilently(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (RuntimeException exception) {
            LOGGER.debug("SSE 连接完成时客户端已断开，忽略完成异常", exception);
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
