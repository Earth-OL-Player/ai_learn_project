package com.earth.online.player.ailearn.assistant;

import com.earth.online.player.ailearn.assistant.AssistantRepository.Operation;
import com.earth.online.player.ailearn.assistant.AssistantRepository.Run;
import com.earth.online.player.ailearn.comment.application.CommentService;
import com.earth.online.player.ailearn.comment.interfaces.CreateCommentRequest;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthSupport;
import com.earth.online.player.ailearn.interaction.application.InteractionContentValidator;
import com.earth.online.player.ailearn.interaction.domain.InteractionTextPolicy;
import com.earth.online.player.ailearn.model.application.ResolvedModelEntitlement;
import com.earth.online.player.ailearn.suggestion.application.SuggestionService;
import com.earth.online.player.ailearn.suggestion.domain.SuggestionType;
import com.earth.online.player.ailearn.suggestion.interfaces.CreateSuggestionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 会话归属、执行租约、用户授权和事务幂等。不能在事务内等待模型或页面。 */
@Service
public class AssistantStateService {
    public static final Set<String> PAGES = Set.of("home", "learning-roadmap", "practice-agent", "interview-questions", "suggestions-comments", "profile");
    private static final Set<String> TERMINAL = Set.of("COMPLETED", "FAILED", "CANCELLED", "EXPIRED");
    private final AssistantRepository repo;
    private final AssistantProperties properties;
    private final CommentService comments;
    private final SuggestionService suggestions;

    public AssistantStateService(AssistantRepository repo, AssistantProperties properties, CommentService comments, SuggestionService suggestions) {
        this.repo = repo;
        this.properties = properties;
        this.comments = comments;
        this.suggestions = suggestions;
    }

    static String id() { return UUID.randomUUID().toString().replace("-", ""); }

    static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    static BusinessException conflict(String message) { return new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), message); }

    private long userId() { return AuthSupport.requireCurrentUser().userId(); }

    private void session(String id, boolean lock) {
        List<Long> users = repo.jdbc().query("SELECT user_id FROM assistant_session WHERE id = ? AND expires_at > CURRENT_TIMESTAMP" + (lock ? " FOR UPDATE" : ""), (rs, row) -> rs.getLong(1), id);
        if (users.isEmpty() || users.get(0) != userId()) throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "助手会话不存在或已过期");
    }

    public Run ownedRun(String id, boolean lock) {
        Run run = repo.run(id, lock);
        if (run == null || run.userId() != userId()) throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "助手任务不存在");
        if (!TERMINAL.contains(run.status()) && run.expiresAt().isBefore(LocalDateTime.now())) {
            repo.jdbc().update("UPDATE assistant_run SET status='EXPIRED',execution_epoch=execution_epoch+1 WHERE id=? AND status NOT IN ('COMPLETED','FAILED','CANCELLED','EXPIRED')", id);
            run = repo.run(id, false);
        }
        return run;
    }

    private void active(Run run) {
        if (TERMINAL.contains(run.status()) || run.expiresAt().isBefore(LocalDateTime.now())) throw conflict("任务已结束或过期，请新建会话任务");
    }

    private void client(Run run, String client, int epoch) {
        if (!run.clientInstanceId().equals(client) || run.executionEpoch() != epoch) throw conflict("页面操作已失效，请刷新任务状态后继续");
    }

    private Operation ownedOperation(Run run, String id) {
        Operation op = repo.operation(id, true);
        if (op == null || !op.runId().equals(run.id()) || op.userId() != run.userId()) throw conflict("操作不存在");
        return op;
    }

    private void version(Operation op, int version, String hash) {
        if (op.payloadVersion() != version || !op.payloadHash().equals(hash)) throw conflict("草稿已更改，请确认最新内容");
    }

    @Transactional
    public Map<String, String> createSession() {
        String id = id();
        repo.jdbc().update("INSERT INTO assistant_session(id,user_id,expires_at) VALUES (?,?,?)", id, userId(), LocalDateTime.now().plusDays(Math.max(1, properties.sessionRetentionDays())));
        return Map.of("id", id);
    }

    public Map<String, Object> sessionSnapshot(String id) {
        session(id, false);
        return Map.of("id", id, "messages", repo.messages(id), "runs", repo.runs(id).stream().map(run -> snapshot(ownedRun(run.id(), false))).toList());
    }

    public Map<String, Object> snapshot(Run run) {
        return Map.of("run", run, "operations", repo.operations(run.id()));
    }

    public Map<String, Object> runSnapshot(String id) { return snapshot(ownedRun(id, false)); }

    private JsonNode cleanContext(JsonNode input) {
        ObjectNode context = repo.json().createObjectNode();
        if (input == null) return context;
        String page = input.path("pageKey").asText("home");
        context.put("pageKey", PAGES.contains(page) ? page : "home");
        String tab = input.path("tab").asText("suggestions");
        context.put("tab", "comments".equals(tab) ? "comments" : "suggestions");
        String selected = input.path("selectedParentId").asText("");
        if (selected.matches("[0-9]{1,19}")) context.put("selectedParentId", selected);
        return context;
    }

    @Transactional
    public Run begin(String sessionId, AssistantRequests.Message request, ResolvedModelEntitlement model) {
        long user = userId();
        // 用户行短事务锁串行化多个标签页的新任务，不锁住模型调用。
        repo.jdbc().queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, user);
        session(sessionId, true);
        JsonNode context = cleanContext(request.pageContext());
        String requestHash = hash(sessionId + "\n" + request.content() + "\n" + repo.write(context));
        Run previous = repo.request(user, request.clientRequestId());
        if (previous != null) {
            if (!previous.requestHash().equals(requestHash)) throw conflict("同一个请求 ID 不能提交不同内容");
            return previous;
        }
        repo.jdbc().update("UPDATE assistant_run SET status = 'EXPIRED',execution_epoch = execution_epoch + 1 WHERE user_id = ? AND status NOT IN ('COMPLETED','FAILED','CANCELLED','EXPIRED') AND expires_at <= CURRENT_TIMESTAMP", user);
        Integer count = repo.jdbc().queryForObject("SELECT COUNT(*) FROM assistant_run WHERE user_id = ? AND status NOT IN ('COMPLETED','FAILED','CANCELLED','EXPIRED')", Integer.class, user);
        if (count != null && count > 0) throw conflict("还有一个助手任务未结束，请先继续或停止该任务");
        if (repo.messages(sessionId).size() > 100) throw conflict("当前会话较长，请开始新会话");
        String id = id();
        LocalDateTime now = LocalDateTime.now();
        String fingerprint = modelFingerprint(model);
        repo.jdbc().update("""
                INSERT INTO assistant_run(id,session_id,user_id,client_request_id,client_instance_id,request_hash,
                  content,page_context_json,status,model_fingerprint,model_name,lease_expires_at,expires_at)
                VALUES (?,?,?,?,?,?,?,?,'READY',?,?,?,?)
                """, id, sessionId, user, request.clientRequestId(), request.clientInstanceId(), requestHash,
                request.content(), repo.write(context), fingerprint, model.modelName(), now,
                now.plusMinutes(Math.max(1, properties.confirmationTtlMinutes())));
        Run run = repo.run(id, false);
        // 结束任务可能在工具请求已保存、结果尚未保存时发生；补齐协议后再开始新用户轮次。
        settlePending(run, "上轮操作已经停止或过期，未完成操作不会自动执行");
        repo.message(run, Map.of("role", "user", "content", request.content()));
        repo.jdbc().update("UPDATE assistant_session SET updated_at = CURRENT_TIMESTAMP WHERE id = ?", sessionId);
        return run;
    }

    static String modelFingerprint(ResolvedModelEntitlement model) {
        return hash(model.level().name() + "|" + model.modelName() + "|" + model.requestConfig().configFingerprint());
    }

    @Transactional
    public Run claim(String id, String clientId, int epoch) {
        Run run = ownedRun(id, true);
        active(run);
        client(run, clientId, epoch);
        if (!"READY".equals(run.status())) throw conflict("任务当前不能执行，请查看操作状态");
        repo.jdbc().update("UPDATE assistant_run SET status = 'RUNNING',lease_expires_at = ?,updated_at = CURRENT_TIMESTAMP WHERE id = ?", LocalDateTime.now().plusSeconds(Math.max(10, properties.runTimeoutSeconds())), id);
        return repo.run(id, false);
    }

    @Transactional
    public void appendModelResult(String runId, int epoch, JsonNode result) {
        Run run = ownedRun(runId, true);
        client(run, run.clientInstanceId(), epoch);
        if (!"RUNNING".equals(run.status())) throw conflict("任务已暂停或停止");
        if (run.leaseExpiresAt().isBefore(LocalDateTime.now())) throw conflict("助手执行超时，请停止后重新发送");
        for (JsonNode message : result.path("messages")) repo.message(run, message);
    }

    @Transactional
    public Operation prepare(String runId, int epoch, JsonNode call) {
        Run run = ownedRun(runId, true);
        if (!"RUNNING".equals(run.status()) || run.executionEpoch() != epoch) throw conflict("任务已暂停或停止");
        if (run.toolCount() >= Math.max(1, properties.maxToolCalls())) throw conflict("已达到本轮操作上限");
        String type = call.path("name").asText();
        JsonNode payload = call.path("args").deepCopy();
        ArrayNode steps = repo.json().createArrayNode();
        boolean authorized = true;
        if ("navigate_page".equals(type)) {
            if (!PAGES.contains(payload.path("pageKey").asText())) throw new BusinessException("页面未接入助手");
            steps.add("navigate");
        } else {
            if (!Set.of("create_comment", "create_suggestion", "prepare_community_draft").contains(type)) throw new BusinessException("操作未接入页面执行器");
            String content = InteractionTextPolicy.normalize(payload.path("content").asText());
            InteractionContentValidator.validatePlainTextContent(content, "内容");
            ((ObjectNode) payload).put("content", content);
            String kind = "create_suggestion".equals(type) ? "suggestion" : payload.path("parentId").asText("").isBlank() ? "comment" : "reply";
            if ("prepare_community_draft".equals(type)) kind = payload.path("kind").asText();
            if (!Set.of("comment", "reply", "suggestion").contains(kind)) throw new BusinessException("表单类型不正确");
            if ("reply".equals(kind)) parentId(payload);
            if ("suggestion".equals(kind)) suggestionType(payload);
            ((ObjectNode) payload).put("kind", kind);
            boolean publish = !"prepare_community_draft".equals(type);
            // 只将当前指令里明确引用的原文视为直接发布授权，其余填写后让用户确认。
            authorized = !publish || explicitlyAuthorized(run, payload, kind);
            steps.add("navigate").add("wait_ready");
            if ("reply".equals(kind)) steps.add("open_reply");
            steps.add("reveal").add("focus");
            if ("suggestion".equals(kind)) steps.add("select_option");
            steps.add("fill").add("validate");
            if (publish) steps.add("submit");
        }
        String id = id();
        repo.jdbc().update("""
                INSERT INTO assistant_operation(id,run_id,user_id,tool_call_id,type,payload_json,payload_hash,status,authorized,action_plan_json,expires_at)
                VALUES (?,?,?,?,?,?,?,'PENDING',?,?,?)
                """, id, runId, run.userId(), call.path("id").asText(), type, repo.write(payload), hash(repo.write(payload)), authorized, repo.write(steps), run.expiresAt());
        repo.jdbc().update("UPDATE assistant_run SET tool_count = tool_count + 1,status = 'WAITING_CLIENT' WHERE id = ?", runId);
        return repo.operation(id, false);
    }

    private boolean explicitlyAuthorized(Run run, JsonNode payload, String kind) {
        String input = run.content();
        if (Pattern.compile("不要|别发|先不|仅填|只填|草稿|写一条|帮我写").matcher(input).find()) return false;
        if (!Pattern.compile("^(?:请|麻烦|帮我|替我|给我|直接|现在|再|一下|\\s|，|,)*(?:提交|发布|发表|发一条|发条|发个|回复)").matcher(input).find()) return false;
        String content = payload.path("content").asText();
        boolean quoted = input.contains("“" + content + "”") || input.contains("\"" + content + "\"") || input.contains("‘" + content + "’") || input.contains("'" + content + "'");
        if (!quoted) return false;
        if ("reply".equals(kind) && !payload.path("parentId").asText().equals(run.pageContext().path("selectedParentId").asText())) return false;
        if ("suggestion".equals(kind)) {
            String label = SuggestionType.valueOf(payload.path("type").asText()).text();
            return input.contains(label) || input.contains(payload.path("type").asText());
        }
        return input.contains("评论") || "reply".equals(kind);
    }

    private Long parentId(JsonNode payload) {
        try {
            long id = Long.parseLong(payload.path("parentId").asText());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) { throw new BusinessException("请明确选择一个父评论再回复"); }
    }

    private String suggestionType(JsonNode payload) {
        try { return SuggestionType.valueOf(payload.path("type").asText()).name(); }
        catch (IllegalArgumentException e) { throw new BusinessException("请明确选择建议类型"); }
    }

    void toolResult(Run run, String callId, Object result) {
        repo.message(run, Map.of("role", "tool", "tool_call_id", callId, "content", repo.write(result)));
    }

    @Transactional
    public void readResult(String runId, int epoch, String callId, Object result) {
        Run run = ownedRun(runId, true);
        if (!"RUNNING".equals(run.status()) || run.executionEpoch() != epoch) throw conflict("任务已暂停或停止");
        toolResult(run, callId, result);
        repo.jdbc().update("UPDATE assistant_run SET tool_count = tool_count + 1 WHERE id = ?", runId);
    }

    private void finishOperation(Run run, Operation op, Object result, String status) {
        repo.jdbc().update("UPDATE assistant_operation SET status = ?,result_json = ? WHERE id = ?", status, repo.write(result), op.id());
        toolResult(run, op.toolCallId(), result);
        repo.status(run.id(), "READY");
    }

    @Transactional
    public Map<String, Object> acknowledge(String runId, AssistantRequests.Action request) {
        Run run = ownedRun(runId, true);
        Operation op = ownedOperation(run, request.operationId());
        version(op, request.payloadVersion(), request.payloadHash());
        if (!"PENDING".equals(op.status())) return snapshot(run);
        active(run);
        client(run, request.clientInstanceId(), request.executionEpoch());
        if (!"WAITING_CLIENT".equals(run.status())) throw conflict("任务当前不能操作页面");
        if (request.stepIndex() < op.nextStep()) return snapshot(run);
        if (request.stepIndex() != op.nextStep()) throw conflict("页面步骤顺序错误");
        if ("submit".equals(op.steps().get(op.nextStep()).asText())) throw conflict("发布必须通过提交接口");
        if ("paused".equals(request.status())) {
            repo.jdbc().update("UPDATE assistant_run SET status='PAUSED',execution_epoch=execution_epoch+1 WHERE id=?", runId);
        } else if ("failed".equals(request.status())) {
            finishOperation(run, op, Map.of("success", false, "message", request.reason() == null ? "页面操作失败" : request.reason()), "FAILED");
        } else if ("completed".equals(request.status())) {
            int next = op.nextStep() + 1;
            repo.jdbc().update("UPDATE assistant_operation SET next_step=? WHERE id=?", next, op.id());
            if (next == op.steps().size()) {
                finishOperation(run, op, Map.of("success", true, "message", "navigate_page".equals(op.type()) ? "页面已打开" : "内容已填写，尚未发布"), "COMPLETED");
            } else if ("submit".equals(op.steps().get(next).asText()) && !op.authorized()) {
                repo.status(runId, "WAITING_CONFIRMATION");
            }
        } else throw new BusinessException("页面状态不正确");
        return snapshot(repo.run(runId, false));
    }

    @Transactional
    public Object submit(String runId, AssistantRequests.Action request) {
        Run run = ownedRun(runId, true);
        Operation op = ownedOperation(run, request.operationId());
        version(op, request.payloadVersion(), request.payloadHash());
        if ("COMPLETED".equals(op.status())) return op.result();
        active(run);
        client(run, request.clientInstanceId(), request.executionEpoch());
        if (!"WAITING_CLIENT".equals(run.status()) || !op.authorized() || !"PENDING".equals(op.status())
                || op.nextStep() != request.stepIndex() || !"submit".equals(op.steps().path(op.nextStep()).asText())) throw conflict("操作未授权或页面步骤尚未完成");
        JsonNode payload = op.payload();
        InteractionContentValidator.validatePlainTextContent(payload.path("content").asText(), "内容");
        Object created;
        if ("create_comment".equals(op.type())) {
            created = comments.create(new CreateCommentRequest(payload.path("content").asText(), "reply".equals(payload.path("kind").asText()) ? parentId(payload) : null));
        } else if ("create_suggestion".equals(op.type())) {
            created = suggestions.create(new CreateSuggestionRequest(suggestionType(payload), payload.path("content").asText()));
        } else throw conflict("该操作不能发布");
        Map<String, Object> result = Map.of("success", true, "message", "发布成功", "kind", payload.path("kind").asText(), "record", created);
        // 业务服务使用 REQUIRED 事务，与操作结果原子提交；失败时全部回滚。
        finishOperation(run, op, result, "COMPLETED");
        return result;
    }

    @Transactional
    public Map<String, Object> confirm(String runId, AssistantRequests.Confirmation request) {
        Run run = ownedRun(runId, true);
        Operation op = ownedOperation(run, request.operationId());
        active(run);
        client(run, request.clientInstanceId(), request.executionEpoch());
        version(op, request.payloadVersion(), request.payloadHash());
        if (!"WAITING_CONFIRMATION".equals(run.status()) || !"PENDING".equals(op.status())) throw conflict("当前没有待确认操作");
        if (request.confirmed()) {
            repo.jdbc().update("UPDATE assistant_operation SET authorized=TRUE WHERE id=?", op.id());
            repo.status(runId, "READY");
        } else {
            finishOperation(run, op, Map.of("success", false, "message", "用户取消发布，草稿保留在表单"), "CANCELLED");
            repo.status(runId, "CANCELLED");
        }
        return snapshot(repo.run(runId, false));
    }

    @Transactional
    public Map<String, Object> control(String runId, AssistantRequests.Control request, String action) {
        Run run = ownedRun(runId, true);
        if (TERMINAL.contains(run.status())) return snapshot(run);
        active(run);
        if (!"resume".equals(action)) client(run, request.clientInstanceId(), request.executionEpoch());
        if ("resume".equals(action)) {
            if ("RUNNING".equals(run.status()) && run.leaseExpiresAt().isAfter(LocalDateTime.now())) throw conflict("模型正在执行，请稍后恢复");
            repo.jdbc().update("UPDATE assistant_run SET status='READY',client_instance_id=?,execution_epoch=execution_epoch+1 WHERE id=?", request.clientInstanceId(), runId);
        } else {
            repo.jdbc().update("UPDATE assistant_run SET status=?,execution_epoch=execution_epoch+1 WHERE id=?", "pause".equals(action) ? "PAUSED" : "CANCELLED", runId);
            if (!"pause".equals(action)) {
                for (Operation op : repo.operations(runId)) if ("PENDING".equals(op.status())) {
                    repo.jdbc().update("UPDATE assistant_operation SET authorized=FALSE,status='CANCELLED' WHERE id=?", op.id());
                    toolResult(run, op.toolCallId(), Map.of("success", false, "message", "用户停止操作"));
                }
                settlePending(run, "用户停止操作");
            }
        }
        return snapshot(repo.run(runId, false));
    }

    @Transactional
    public Map<String, Object> draft(String runId, AssistantRequests.Draft request) {
        Run run = ownedRun(runId, true);
        active(run);
        client(run, request.clientInstanceId(), request.executionEpoch());
        Operation op = ownedOperation(run, request.operationId());
        if (!Set.of("PAUSED", "WAITING_CONFIRMATION").contains(run.status()) || !"PENDING".equals(op.status()) || op.payloadVersion() != request.payloadVersion()) throw conflict("当前不能修改草稿");
        if ("navigate_page".equals(op.type())) throw conflict("导航没有草稿");
        String content = InteractionTextPolicy.normalize(request.content());
        InteractionContentValidator.validatePlainTextContent(content, "内容");
        ObjectNode payload = op.payload().deepCopy();
        payload.put("content", content);
        repo.jdbc().update("UPDATE assistant_operation SET payload_json=?,payload_hash=?,payload_version=payload_version+1,authorized=FALSE,next_step=0 WHERE id=?", repo.write(payload), hash(repo.write(payload)), op.id());
        repo.status(runId, "PAUSED");
        return snapshot(repo.run(runId, false));
    }

    @Transactional
    public void dispatch(String runId, int epoch) {
        Run run = ownedRun(runId, true);
        if (!"RUNNING".equals(run.status()) || run.executionEpoch() != epoch) throw conflict("任务已暂停");
        Operation op = repo.operations(runId).stream().filter(item -> "PENDING".equals(item.status())).findFirst().orElseThrow();
        repo.status(runId, "submit".equals(op.steps().path(op.nextStep()).asText()) && !op.authorized() ? "WAITING_CONFIRMATION" : "WAITING_CLIENT");
    }

    @Transactional
    public void finish(String runId, int epoch, String status) {
        Run run = ownedRun(runId, true);
        if ("RUNNING".equals(run.status()) && run.executionEpoch() == epoch) {
            if ("FAILED".equals(status)) settlePending(run, "本轮已结束，该操作没有执行");
            repo.status(runId, status);
        }
    }

    private void settlePending(Run run, String message) {
        var calls = new java.util.LinkedHashMap<String, JsonNode>();
        for (JsonNode item : repo.messages(run.sessionId())) {
            if ("user".equals(item.path("role").asText())) calls.clear();
            if ("assistant".equals(item.path("role").asText())) for (JsonNode call : item.path("tool_calls")) calls.put(call.path("id").asText(), call);
            if ("tool".equals(item.path("role").asText())) calls.remove(item.path("tool_call_id").asText());
        }
        for (String callId : calls.keySet()) toolResult(run, callId, Map.of("success", false, "message", message));
    }
}
