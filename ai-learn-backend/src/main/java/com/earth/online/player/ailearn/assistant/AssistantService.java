package com.earth.online.player.ailearn.assistant;

import com.earth.online.player.ailearn.assistant.AssistantRepository.Run;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.comment.application.CommentService;
import com.earth.online.player.ailearn.growth.application.GrowthService;
import com.earth.online.player.ailearn.model.application.ModelEntitlementService;
import com.earth.online.player.ailearn.suggestion.application.SuggestionService;
import com.earth.online.player.ailearn.user.application.UserQuestionStatsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Service;

/** 模型与受控工具循环。等待页面时结束流，释放线程，续跑从数据库状态恢复。 */
@Service
public class AssistantService {
    private final AssistantStateService state;
    private final AssistantRepository repo;
    private final AssistantAiClient ai;
    private final ModelEntitlementService models;
    private final CommentService comments;
    private final SuggestionService suggestions;
    private final GrowthService growth;
    private final UserQuestionStatsService stats;
    private final AssistantProperties properties;

    public AssistantService(AssistantStateService state, AssistantRepository repo, AssistantAiClient ai,
            ModelEntitlementService models, CommentService comments, SuggestionService suggestions,
            GrowthService growth, UserQuestionStatsService stats, AssistantProperties properties) {
        this.state = state; this.repo = repo; this.ai = ai; this.models = models; this.comments = comments;
        this.suggestions = suggestions; this.growth = growth; this.stats = stats; this.properties = properties;
    }

    public Run begin(String sessionId, AssistantRequests.Message request) {
        return state.begin(sessionId, request, models.resolveForAiCall(com.earth.online.player.ailearn.common.security.AuthSupport.requireCurrentUser().userId()));
    }

    private List<JsonNode> pending(List<JsonNode> messages) {
        Set<String> resolved = new HashSet<>();
        List<JsonNode> calls = new ArrayList<>();
        for (JsonNode message : messages) {
            if ("user".equals(message.path("role").asText())) { calls.clear(); resolved.clear(); }
            if ("assistant".equals(message.path("role").asText())) message.path("tool_calls").forEach(calls::add);
            if ("tool".equals(message.path("role").asText())) resolved.add(message.path("tool_call_id").asText());
        }
        return calls.stream().filter(call -> !resolved.contains(call.path("id").asText())).toList();
    }

    public void execute(String runId, String client, int epoch, BiConsumer<String, Object> emit) {
        Run run = state.claim(runId, client, epoch);
        emit.accept("meta", state.snapshot(run));
        long deadline = System.nanoTime() + Math.max(10, properties.runTimeoutSeconds()) * 1_000_000_000L;
        try {
            var model = models.resolveForAiCall(run.userId());
            if (!run.modelFingerprint().equals(AssistantStateService.modelFingerprint(model))) throw new BusinessException("模型权益或配置已变化，请停止当前任务后重新发送");
            var existing = repo.operations(runId).stream().filter(op -> "PENDING".equals(op.status())).findFirst();
            if (existing.isPresent()) {
                state.dispatch(runId, epoch);
                emit.accept("client_action", state.runSnapshot(runId));
                emit.accept("done", state.runSnapshot(runId));
                return;
            }
            while (System.nanoTime() < deadline) {
                run = state.ownedRun(runId, false);
                if (!"RUNNING".equals(run.status()) || run.executionEpoch() != epoch) return;
                List<JsonNode> messages = repo.messages(run.sessionId());
                List<JsonNode> calls = pending(messages);
                if (!calls.isEmpty()) {
                    if (calls.size() > 1) {
                        for (JsonNode call : calls) state.readResult(runId, epoch, call.path("id").asText(), Map.of("success", false, "message", "请每次调用一个业务工具，按顺序执行"));
                        continue;
                    }
                    JsonNode call = calls.get(0);
                    if (run.toolCount() >= Math.max(1, properties.maxToolCalls())) throw new BusinessException("已达到本轮工具调用上限，请新建任务");
                    String type = call.path("name").asText();
                    emit.accept("tool_started", Map.of("name", type));
                    if (Set.of("navigate_page", "prepare_community_draft", "create_comment", "create_suggestion").contains(type)) {
                        try {
                            state.prepare(runId, epoch, call);
                        } catch (BusinessException e) {
                            state.readResult(runId, epoch, call.path("id").asText(), Map.of("success", false, "message", e.getMessage()));
                            continue;
                        }
                        emit.accept("client_action", state.runSnapshot(runId));
                        emit.accept("done", state.runSnapshot(runId));
                        return;
                    }
                    Object result;
                    try { result = readTool(type, call.path("args")); }
                    catch (BusinessException e) { result = Map.of("success", false, "message", e.getMessage()); }
                    state.readResult(runId, epoch, call.path("id").asText(), result);
                    emit.accept("tool_result", Map.of("name", type, "data", result));
                    continue;
                }
                JsonNode result = ai.step(messages, run.pageContext(), model.requestConfig(), text -> emit.accept("text_delta", Map.of("text", text)));
                // 来源映射只允许真实站内路由，协议消息里保存可展示来源以供刷新恢复。
                var sources = repo.json().createArrayNode();
                for (JsonNode source : result.path("sources")) if (AssistantStateService.PAGES.contains(source.path("path").asText().replaceFirst("^/", ""))) sources.add(source);
                for (JsonNode message : result.path("messages")) if ("assistant".equals(message.path("role").asText())) ((ObjectNode) message).set("sources", sources);
                state.appendModelResult(runId, epoch, result);
                emit.accept("model_result", result);
                if (pending(repo.messages(run.sessionId())).isEmpty()) {
                    state.finish(runId, epoch, "COMPLETED");
                    emit.accept("done", state.runSnapshot(runId));
                    return;
                }
            }
            throw new BusinessException("助手执行超时，已停止后续操作");
        } catch (Exception e) {
            state.finish(runId, epoch, "FAILED");
            emit.accept("error", Map.of("message", e instanceof BusinessException ? e.getMessage() : "助手任务执行失败，请稍后重试"));
            emit.accept("done", state.runSnapshot(runId));
        }
    }

    private Object readTool(String type, JsonNode args) {
        return switch (type) {
            case "list_comments" -> comments.findPage(args.path("pageNo").asInt(1), Math.min(20, args.path("pageSize").asInt(10)), args.path("sort").asText("latest"));
            case "list_suggestions" -> suggestions.findPage(args.path("pageNo").asInt(1), Math.min(20, args.path("pageSize").asInt(10)), args.path("sort").asText("latest"));
            case "get_my_model_entitlement" -> models.getCurrentStatus();
            case "get_my_growth" -> growth.getCurrentGrowth();
            case "get_my_practice_stats" -> stats.getCurrentUserStatsOverview();
            case "search_site_knowledge" -> Map.of("message", "请依据已提供的知识片段回答");
            default -> throw new BusinessException("该工具未接入本站");
        };
    }
}
