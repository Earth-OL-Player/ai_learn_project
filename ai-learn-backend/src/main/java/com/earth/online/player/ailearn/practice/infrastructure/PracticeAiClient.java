package com.earth.online.player.ailearn.practice.infrastructure;

import com.earth.online.player.ailearn.ai.AiServiceProperties;
import com.earth.online.player.ailearn.answer.domain.GradingResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI 服务刷题客户端。
 */
@Component
public class PracticeAiClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final Logger LOGGER = LoggerFactory.getLogger(PracticeAiClient.class);

    private final AiServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * 创建 AI 服务客户端。
     *
     * @param properties AI 服务配置
     * @param objectMapper JSON 序列化器
     */
    public PracticeAiClient(AiServiceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        // Uvicorn 不支持 Java HttpClient 默认的明文 HTTP/2 升级请求，固定 HTTP/1.1 保证请求体稳定送达。
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * 请求 AI 服务评分。
     *
     * @param userId 用户ID
     * @param question 题目
     * @param userAnswer 用户答案
     * @return 评分结果
     */
    public Optional<PracticeAiGradingResult> grade(Long userId, PracticeQuestionRecord question, String userAnswer) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("userId", String.valueOf(userId));
            payload.put("questionCode", question.getCode());
            payload.put("question", question.getQuestion());
            payload.put("questionType", question.getQuestionType());
            payload.put("standardAnswer", question.getStandardAnswer());
            payload.put("userAnswer", userAnswer);

            // AI 服务异常时返回空结果，由后端本地规则兜底。
            JsonNode data = postJson("/internal/v1/practice/answer/grade", payload).orElse(null);
            if (data == null) {
                return Optional.empty();
            }
            return Optional.of(toPracticeAiGradingResult(data));
        } catch (RuntimeException exception) {
            LOGGER.warn("AI 服务评分结果转换失败，已切换后端本地评分：questionCode={}", question.getCode(), exception);
            return Optional.empty();
        }
    }

    /**
     * 请求 AI 服务进行本题讨论。
     *
     * @param question 当前题目
     * @param message 用户消息
     * @return 讨论回复
     */
    public Optional<String> discuss(PracticeQuestionRecord question, String lastUserAnswer, String message) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("questionCode", question.getCode());
            payload.put("question", question.getQuestion());
            payload.put("questionType", question.getQuestionType());
            payload.put("standardAnswer", question.getStandardAnswer());
            payload.put("lastUserAnswer", lastUserAnswer == null ? "" : lastUserAnswer);
            payload.put("message", message);
            JsonNode data = postJson("/internal/v1/practice/discuss", payload).orElse(null);
            if (data == null || !data.hasNonNull("reply")) {
                return Optional.empty();
            }
            return Optional.of(data.get("reply").asText());
        } catch (RuntimeException exception) {
            LOGGER.warn("AI 服务讨论结果转换失败，已切换后端本地讨论：questionCode={}", question.getCode(), exception);
            return Optional.empty();
        }
    }

    /**
     * 请求 AI 服务判断用户输入是否与当前刷题上下文相关。
     *
     * @param question 当前题目
     * @param phase 当前阶段
     * @param message 用户消息
     * @return 相关性判断
     */
    public Optional<Boolean> judgeRelevance(PracticeQuestionRecord question, String phase, String message) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("questionCode", question.getCode());
            payload.put("question", question.getQuestion());
            payload.put("questionType", question.getQuestionType());
            payload.put("standardAnswer", question.getStandardAnswer());
            payload.put("phase", phase);
            payload.put("message", message);

            // AI 服务不可用时返回空，由后端保留关键词兜底拦截。
            JsonNode data = postJson("/internal/v1/practice/relevance", payload).orElse(null);
            if (data == null || !data.hasNonNull("relevant")) {
                return Optional.empty();
            }
            return Optional.of(data.get("relevant").asBoolean(true));
        } catch (RuntimeException exception) {
            LOGGER.warn("AI 服务相关性判断失败，已切换关键词兜底：questionCode={}", question.getCode(), exception);
            return Optional.empty();
        }
    }

    /**
     * 发送 JSON 请求。
     *
     * @param path 请求路径
     * @param payload 请求体
     * @return 响应 data 节点
     */
    private Optional<JsonNode> postJson(String path, JsonNode payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl() + path))
                    .timeout(timeout())
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Content-Type", JSON_CONTENT_TYPE)
                    .header("Accept", JSON_CONTENT_TYPE)
                    .header(INTERNAL_TOKEN_HEADER, properties.getToken())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn("AI 服务调用返回非成功状态，已进入本地兜底：path={} status={}", path, response.statusCode());
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (!"SUCCESS".equals(root.path("code").asText())) {
                LOGGER.warn("AI 服务业务响应失败，已进入本地兜底：path={} code={}", path, root.path("code").asText());
                return Optional.empty();
            }
            return Optional.ofNullable(root.get("data"));
        } catch (Exception exception) {
            LOGGER.warn("AI 服务调用异常，已进入本地兜底：path={}", path, exception);
            return Optional.empty();
        }
    }

    /**
     * 转换 AI 服务评分包装结果。
     *
     * @param data 响应节点
     * @return 评分结果
     */
    private PracticeAiGradingResult toPracticeAiGradingResult(JsonNode data) {
        GradingResult gradingResult = new GradingResult(
                clampScore(data.path("score").asInt(0)),
                data.path("correct").asBoolean(false),
                readStringList(data.get("hitPoints")),
                readStringList(data.get("missingPoints")),
                readStringList(data.get("problems")),
                data.path("referenceAnswer").asText(""),
                data.path("improvementAdvice").asText(""),
                readStringList(data.get("reviewKnowledgePoints"))
        );

        // AI 服务会显式告诉后端本次评分是否来自本地规则兜底。
        return new PracticeAiGradingResult(gradingResult, data.path("fallbackUsed").asBoolean(false));
    }

    /**
     * 读取字符串数组。
     *
     * @param node JSON 节点
     * @return 字符串列表
     */
    private List<String> readStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (StringUtils.hasText(item.asText())) {
                values.add(item.asText());
            }
        }
        return values;
    }

    /**
     * 判断 AI 服务是否启用。
     *
     * @return 是否启用
     */
    private boolean isEnabled() {
        return properties.isEnabled() && StringUtils.hasText(properties.getBaseUrl());
    }

    /**
     * 规整基础地址。
     *
     * @return 基础地址
     */
    private String normalizeBaseUrl() {
        return properties.getBaseUrl().replaceAll("/+$", "");
    }

    /**
     * 获取安全超时时间。
     *
     * @return 超时时间
     */
    private Duration timeout() {
        return Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds()));
    }

    /**
     * 限制得分范围。
     *
     * @param score 原始得分
     * @return 安全得分
     */
    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
