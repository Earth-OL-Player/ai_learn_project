package com.earth.online.player.ailearn.ai;

import com.earth.online.player.ailearn.common.trace.TraceContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI 服务模型缓存内部客户端。
 */
@Component
public class AiModelCacheClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiModelCacheClient.class);
    private static final int MAX_INVALIDATE_TIMEOUT_SECONDS = 3;

    private final AiServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * 创建 AI 服务模型缓存客户端。
     *
     * @param properties AI 服务配置
     * @param objectMapper JSON 序列化器
     */
    public AiModelCacheClient(AiServiceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        // Uvicorn 内部接口使用 HTTP/1.1，避免 Java HttpClient 默认升级影响本地服务。
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * 通知 AI 服务清理模型和 Agent 构造缓存。
     */
    public void invalidateModelCache() {
        if (!hasEndpoint()) {
            return;
        }
        try {
            HttpRequest request = addCommonHeaders(HttpRequest.newBuilder()
                    .uri(URI.create(endpoint()))
                    .timeout(timeout())
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Accept", AiServiceConstants.JSON_CONTENT_TYPE)
                    .POST(HttpRequest.BodyPublishers.noBody()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logInvalidationResult(response);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("AI 服务模型缓存失效通知被中断，已忽略本次通知", exception);
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.warn("AI 服务模型缓存失效通知失败，后续请求仍会通过配置指纹使用新配置", exception);
        }
    }

    /**
     * 记录缓存失效结果。
     *
     * @param response HTTP 响应
     */
    private void logInvalidationResult(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            LOGGER.warn("AI 服务模型缓存失效通知返回非成功状态：status={}", response.statusCode());
            return;
        }
        if (!isSuccessBody(response.body())) {
            LOGGER.warn("AI 服务模型缓存失效通知返回业务失败响应");
        }
    }

    /**
     * 判断统一响应是否成功。
     *
     * @param body 响应体
     * @return 是否成功
     */
    private boolean isSuccessBody(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            return AiServiceConstants.SUCCESS_CODE.equals(root.path("code").asText());
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * 补充 AI 服务内部调用请求头。
     *
     * @param builder 请求构造器
     * @return 请求构造器
     */
    private HttpRequest.Builder addCommonHeaders(HttpRequest.Builder builder) {
        builder.header(AiServiceConstants.INTERNAL_TOKEN_HEADER, properties.getToken());
        String traceId = TraceContext.getTraceId();
        if (StringUtils.hasText(traceId)) {
            builder.header(TraceContext.TRACE_ID_HEADER, traceId);
        }
        return builder;
    }

    /**
     * 判断 AI 服务模型缓存接口是否可调用。
     *
     * @return 是否可调用
     */
    private boolean hasEndpoint() {
        return properties.isEnabled() && StringUtils.hasText(properties.getBaseUrl());
    }

    /**
     * 获取模型缓存失效接口地址。
     *
     * @return 接口地址
     */
    private String endpoint() {
        return properties.getBaseUrl().replaceAll("/+$", "") + AiServiceConstants.MODEL_CACHE_INVALIDATE_PATH;
    }

    /**
     * 获取安全超时时间。
     *
     * @return 超时时间
     */
    private Duration timeout() {
        int safeTimeoutSeconds = Math.min(MAX_INVALIDATE_TIMEOUT_SECONDS, Math.max(1, properties.getTimeoutSeconds()));
        return Duration.ofSeconds(safeTimeoutSeconds);
    }
}
