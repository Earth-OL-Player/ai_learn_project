package com.earth.online.player.ailearn.common.logging.infrastructure;

import com.earth.online.player.ailearn.ai.AiServiceConstants;
import com.earth.online.player.ailearn.ai.AiServiceHttpSupport;
import com.earth.online.player.ailearn.ai.AiServiceProperties;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.logging.ManagedLogService;
import com.earth.online.player.ailearn.common.logging.interfaces.LogLevelResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.util.DateTimeUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI 服务日志级别内部客户端。
 */
@Component
public class AiServiceLogLevelClient {

    private static final String DATA_FIELD = "data";
    private static final String LEVEL_FIELD = "level";
    private static final String MESSAGE_FIELD = "message";
    private static final String DEFAULT_SUCCESS_MESSAGE = "AI 服务日志级别已生效";
    private static final String DEFAULT_READY_MESSAGE = "AI 服务日志级别可动态调整";

    private final AiServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * 创建 AI 服务日志级别客户端。
     *
     * @param properties AI 服务配置
     * @param objectMapper JSON 序列化器
     */
    public AiServiceLogLevelClient(AiServiceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = AiServiceHttpSupport.newHttpClient(timeout());
    }

    /**
     * 查询 AI 服务日志级别。
     *
     * @return AI 服务日志级别，服务不可达时为空
     */
    public Optional<LogLevelResponse> find() {
        if (!hasEndpoint()) {
            return Optional.empty();
        }
        try {
            HttpRequest request = AiServiceHttpSupport.newInternalRequestBuilder(properties, timeout(), endpoint())
                    .header("Accept", AiServiceConstants.JSON_CONTENT_TYPE)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseSuccessResponse(response.body(), DEFAULT_READY_MESSAGE);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /**
     * 更新 AI 服务日志级别。
     *
     * @param level 日志级别
     * @return 更新后日志级别
     */
    public LogLevelResponse update(String level) {
        if (!hasEndpoint()) {
            throw new BusinessException(ResponseCode.BUSINESS_ERROR.code(), "AI 服务地址未配置，无法更新日志级别");
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put(LEVEL_FIELD, level);
            HttpRequest request = AiServiceHttpSupport.newInternalRequestBuilder(properties, timeout(), endpoint())
                    .header("Content-Type", AiServiceConstants.JSON_CONTENT_TYPE)
                    .header("Accept", AiServiceConstants.JSON_CONTENT_TYPE)
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseSuccessResponse(response.body(), DEFAULT_SUCCESS_MESSAGE)
                    .orElseThrow(() -> new BusinessException(ResponseCode.BUSINESS_ERROR.code(), "AI 服务日志级别更新失败"));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResponseCode.BUSINESS_ERROR.code(), "AI 服务不可用，无法更新日志级别");
        } catch (IOException | IllegalArgumentException exception) {
            throw new BusinessException(ResponseCode.BUSINESS_ERROR.code(), "AI 服务不可用，无法更新日志级别");
        }
    }

    /**
     * 解析 AI 服务统一成功响应。
     *
     * @param body 响应体
     * @param defaultMessage 默认状态说明
     * @return 日志级别响应
     */
    private Optional<LogLevelResponse> parseSuccessResponse(String body, String defaultMessage) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (!AiServiceConstants.SUCCESS_CODE.equals(root.path("code").asText())) {
                return Optional.empty();
            }
            JsonNode data = root.path(DATA_FIELD);
            String level = data.path(LEVEL_FIELD).asText("");
            if (!StringUtils.hasText(level)) {
                return Optional.empty();
            }
            String message = data.path(MESSAGE_FIELD).asText(defaultMessage);
            return Optional.of(new LogLevelResponse(
                    ManagedLogService.AI_SERVICE.code(),
                    ManagedLogService.AI_SERVICE.label(),
                    level,
                    true,
                    StringUtils.hasText(message) ? message : defaultMessage,
                    now()
            ));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    /**
     * 判断 AI 服务端点是否可调用。
     *
     * @return 是否存在基础地址
     */
    private boolean hasEndpoint() {
        return StringUtils.hasText(properties.getBaseUrl());
    }

    /**
     * 构造 AI 服务日志级别端点。
     *
     * @return 完整端点地址
     */
    private String endpoint() {
        return AiServiceHttpSupport.normalizeBaseUrl(properties) + AiServiceConstants.LOG_LEVEL_PATH;
    }

    /**
     * 获取安全超时时间。
     *
     * @return 超时时间
     */
    private Duration timeout() {
        return AiServiceHttpSupport.timeout(properties);
    }

    /**
     * 获取当前偏移时间。
     *
     * @return 当前时间
     */
    private OffsetDateTime now() {
        return DateTimeUtils.currentOffsetDateTime();
    }
}
