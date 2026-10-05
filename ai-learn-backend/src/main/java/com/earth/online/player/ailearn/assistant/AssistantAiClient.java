package com.earth.online.player.ailearn.assistant;

import com.earth.online.player.ailearn.ai.AiServiceHttpSupport;
import com.earth.online.player.ailearn.ai.AiServiceProperties;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.model.domain.AiModelRequestConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/** 内部模型协议适配；超时覆盖完整响应体，凭据不进入持久化或浏览器。 */
@Component
public class AssistantAiClient {
    private final AiServiceProperties properties;
    private final AssistantProperties assistantProperties;
    private final ObjectMapper json;
    private final HttpClient http = AiServiceHttpSupport.newHttpClient(Duration.ofSeconds(10));
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "assistant-stream-timeout");
        thread.setDaemon(true);
        return thread;
    });

    public AssistantAiClient(AiServiceProperties properties, AssistantProperties assistantProperties, ObjectMapper json) {
        this.properties = properties;
        this.assistantProperties = assistantProperties;
        this.json = json;
    }

    public JsonNode step(List<JsonNode> messages, JsonNode page, AiModelRequestConfig model, Consumer<String> chunk) {
        if (!properties.isEnabled()) throw new BusinessException("AI 服务尚未启用，请配置 AI_SERVICE_ENABLED=true");
        try {
            Duration timeout = Duration.ofSeconds(Math.max(10, assistantProperties.runTimeoutSeconds()));
            HttpRequest request = AiServiceHttpSupport.newInternalRequestBuilder(properties, timeout,
                    AiServiceHttpSupport.normalizeBaseUrl(properties) + "/internal/v1/assistant/steps/stream")
                    .header("Content-Type", "application/json").header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("messages", messages, "pageContext", page, "modelConfig", model)), StandardCharsets.UTF_8)).build();
            HttpResponse<java.io.InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var body = response.body()) {
                if (response.statusCode() != 200) throw new BusinessException("AI 服务连接或内部鉴权失败，请检查服务配置");
                var deadline = timer.schedule(() -> { try { body.close(); } catch (Exception ignored) { } }, timeout.toSeconds(), TimeUnit.SECONDS);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
                    String line;
                    String event = "message";
                    StringBuilder data = new StringBuilder();
                    JsonNode result = null;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("event:")) event = line.substring(6).trim();
                        else if (line.startsWith("data:")) data.append(line.substring(5).stripLeading());
                        else if (line.isBlank() && !data.isEmpty()) {
                            JsonNode payload = json.readTree(data.toString());
                            if ("error".equals(event)) throw new BusinessException(payload.path("message").asText("模型暂不可用"));
                            if ("text_delta".equals(event)) chunk.accept(payload.path("text").asText());
                            if ("result".equals(event)) result = payload;
                            data.setLength(0);
                        }
                    }
                    if (result == null) throw new BusinessException("模型未返回完整结果，请重试");
                    return result;
                } finally { deadline.cancel(false); }
            }
        } catch (BusinessException e) { throw e; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new BusinessException("助手任务已中止"); }
        catch (Exception e) { throw new BusinessException("助手模型连接超时或中断，请检查服务后重试"); }
    }

    @PreDestroy
    public void close() { timer.shutdownNow(); }
}
