package com.earth.online.player.ailearn.assistant;

import com.earth.online.player.ailearn.common.config.AiStreamExecutorConfig;
import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthSupport;
import com.earth.online.player.ailearn.common.trace.TraceContext;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 所有助手接口受认证过滤器保护，并再次校验任务归属。 */
@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {
    private final AssistantStateService state;
    private final AssistantService service;
    private final AssistantProperties properties;
    private final Executor executor;

    public AssistantController(AssistantStateService state, AssistantService service, AssistantProperties properties,
            @Qualifier(AiStreamExecutorConfig.AI_STREAM_EXECUTOR_BEAN_NAME) Executor executor) {
        this.state = state; this.service = service; this.properties = properties; this.executor = executor;
    }

    @PostMapping("/sessions")
    public ApiResponse<?> create() { return ApiResponse.success(state.createSession()); }

    @GetMapping("/sessions/{id}")
    public ApiResponse<?> session(@PathVariable String id) { return ApiResponse.success(state.sessionSnapshot(id)); }

    @GetMapping("/runs/{id}")
    public ApiResponse<?> run(@PathVariable String id) { return ApiResponse.success(state.runSnapshot(id)); }

    @PostMapping(value = "/sessions/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter message(@PathVariable String id, @Valid @RequestBody AssistantRequests.Message request) {
        var run = service.begin(id, request);
        if (!"READY".equals(run.status())) return snapshotStream(run.id());
        return stream(run.id(), request.clientInstanceId(), run.executionEpoch());
    }

    @PostMapping(value = "/runs/{id}/continue/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter continueRun(@PathVariable String id, @Valid @RequestBody AssistantRequests.Control request) {
        state.ownedRun(id, false);
        return stream(id, request.clientInstanceId(), request.executionEpoch());
    }

    @PostMapping(value = "/runs/{id}/resume/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter resume(@PathVariable String id, @Valid @RequestBody AssistantRequests.Control request) {
        state.control(id, request, "resume");
        var run = state.ownedRun(id, false);
        return stream(id, request.clientInstanceId(), run.executionEpoch());
    }

    @PostMapping("/runs/{id}/pause")
    public ApiResponse<?> pause(@PathVariable String id, @Valid @RequestBody AssistantRequests.Control request) { return ApiResponse.success(state.control(id, request, "pause")); }

    @PostMapping("/runs/{id}/cancel")
    public ApiResponse<?> cancel(@PathVariable String id, @Valid @RequestBody AssistantRequests.Control request) { return ApiResponse.success(state.control(id, request, "cancel")); }

    @PostMapping("/runs/{id}/client-results")
    public ApiResponse<?> acknowledge(@PathVariable String id, @Valid @RequestBody AssistantRequests.Action request) { return ApiResponse.success(state.acknowledge(id, request)); }

    @PostMapping("/runs/{id}/confirm")
    public ApiResponse<?> confirm(@PathVariable String id, @Valid @RequestBody AssistantRequests.Confirmation request) { return ApiResponse.success(state.confirm(id, request)); }

    @PostMapping("/runs/{id}/operations/{operationId}/submit")
    public ApiResponse<?> submit(@PathVariable String id, @PathVariable String operationId, @Valid @RequestBody AssistantRequests.Action request) {
        if (!operationId.equals(request.operationId())) throw AssistantStateService.conflict("操作标识不一致");
        return ApiResponse.success(state.submit(id, request));
    }

    @PostMapping("/runs/{id}/draft")
    public ApiResponse<?> draft(@PathVariable String id, @Valid @RequestBody AssistantRequests.Draft request) { return ApiResponse.success(state.draft(id, request)); }

    private SseEmitter snapshotStream(String runId) {
        SseEmitter emitter = new SseEmitter(10000L);
        try { emitter.send(SseEmitter.event().name("done").data(state.runSnapshot(runId))); emitter.complete(); }
        catch (Exception e) { emitter.completeWithError(e); }
        return emitter;
    }

    private SseEmitter stream(String runId, String client, int epoch) {
        var user = AuthSupport.requireCurrentUser();
        String traceId = TraceContext.getTraceId();
        SseEmitter emitter = new SseEmitter((Math.max(10, properties.runTimeoutSeconds()) + 10) * 1000L);
        AtomicBoolean closed = new AtomicBoolean();
        emitter.onCompletion(() -> closed.set(true));
        emitter.onTimeout(() -> closed.set(true));
        emitter.onError(error -> closed.set(true));
        try {
            executor.execute(() -> {
                AuthContext.setUser(user);
                TraceContext.setTraceId(traceId);
                try {
                    service.execute(runId, client, epoch, (kind, payload) -> {
                        if (closed.get()) throw new IllegalStateException("客户端已断开");
                        try { emitter.send(SseEmitter.event().name(kind).data(payload)); }
                        catch (Exception e) { closed.set(true); throw new IllegalStateException("客户端已断开"); }
                    });
                    emitter.complete();
                } catch (Exception e) {
                    // 不打印模型正文、凭据或私人聊天；断线后由查询接口恢复状态。
                    if (!closed.get()) {
                        try { emitter.send(SseEmitter.event().name("error").data(Map.of("message", "任务连接已中断，请查看状态后继续"))); }
                        catch (Exception ignored) { }
                    }
                    emitter.complete();
                } finally { AuthContext.clear(); TraceContext.clear(); }
            });
        } catch (RuntimeException e) { state.finish(runId, epoch, "FAILED"); throw e; }
        return emitter;
    }
}
