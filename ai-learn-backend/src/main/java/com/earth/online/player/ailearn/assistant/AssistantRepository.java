package com.earth.online.player.ailearn.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** 助手持久化。JSON 不包含模型凭据，事务锁由应用服务统一管理。 */
@Repository
public class AssistantRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public AssistantRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public JdbcTemplate jdbc() { return jdbc; }
    public ObjectMapper json() { return json; }

    public record Run(String id, String sessionId, long userId, String clientRequestId,
                      String clientInstanceId, String requestHash, String content, JsonNode pageContext,
                      String status, String modelFingerprint, String modelName, int executionEpoch,
                      int toolCount, LocalDateTime leaseExpiresAt, LocalDateTime expiresAt) { }

    public record Operation(String id, String runId, long userId, String toolCallId, String type,
                            JsonNode payload, String payloadHash, int payloadVersion, String status,
                            boolean authorized, int nextStep, JsonNode steps, JsonNode result,
                            LocalDateTime expiresAt) { }

    private final RowMapper<Run> runMapper = (rs, row) -> new Run(rs.getString("id"), rs.getString("session_id"),
            rs.getLong("user_id"), rs.getString("client_request_id"), rs.getString("client_instance_id"),
            rs.getString("request_hash"), rs.getString("content"), read(rs.getString("page_context_json")),
            rs.getString("status"), rs.getString("model_fingerprint"), rs.getString("model_name"),
            rs.getInt("execution_epoch"), rs.getInt("tool_count"), rs.getTimestamp("lease_expires_at").toLocalDateTime(),
            rs.getTimestamp("expires_at").toLocalDateTime());

    private final RowMapper<Operation> operationMapper = (rs, row) -> new Operation(rs.getString("id"),
            rs.getString("run_id"), rs.getLong("user_id"), rs.getString("tool_call_id"), rs.getString("type"),
            read(rs.getString("payload_json")), rs.getString("payload_hash"), rs.getInt("payload_version"),
            rs.getString("status"), rs.getBoolean("authorized"), rs.getInt("next_step"),
            read(rs.getString("action_plan_json")), read(rs.getString("result_json")), rs.getTimestamp("expires_at").toLocalDateTime());

    JsonNode read(String value) {
        try { return value == null ? json.nullNode() : json.readTree(value); }
        catch (Exception e) { throw new IllegalStateException("助手状态读取失败"); }
    }

    String write(Object value) {
        try { return json.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalStateException("助手状态保存失败"); }
    }

    Run run(String id, boolean lock) {
        return jdbc.query("SELECT * FROM assistant_run WHERE id = ?" + (lock ? " FOR UPDATE" : ""), runMapper, id)
                .stream().findFirst().orElse(null);
    }

    Run request(long userId, String requestId) {
        return jdbc.query("SELECT * FROM assistant_run WHERE user_id = ? AND client_request_id = ?", runMapper, userId, requestId)
                .stream().findFirst().orElse(null);
    }

    List<Run> runs(String sessionId) {
        return jdbc.query("SELECT * FROM assistant_run WHERE session_id = ? ORDER BY created_at, id", runMapper, sessionId);
    }

    List<Operation> operations(String runId) {
        return jdbc.query("SELECT * FROM assistant_operation WHERE run_id = ? ORDER BY created_at, id", operationMapper, runId);
    }

    Operation operation(String id, boolean lock) {
        return jdbc.query("SELECT * FROM assistant_operation WHERE id = ?" + (lock ? " FOR UPDATE" : ""), operationMapper, id)
                .stream().findFirst().orElse(null);
    }

    List<JsonNode> messages(String sessionId) {
        return jdbc.query("SELECT message_json FROM assistant_message WHERE session_id = ? ORDER BY id", (rs, row) -> read(rs.getString(1)), sessionId);
    }

    void message(Run run, Object message) {
        jdbc.update("INSERT INTO assistant_message(session_id, run_id, message_json) VALUES (?, ?, ?)", run.sessionId(), run.id(), write(message));
    }

    void status(String runId, String status) {
        jdbc.update("UPDATE assistant_run SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", status, runId);
    }
}
