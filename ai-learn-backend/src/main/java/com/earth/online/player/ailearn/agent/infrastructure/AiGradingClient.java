package com.earth.online.player.ailearn.agent.infrastructure;

import com.earth.online.player.ailearn.answer.domain.GradingResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI 服务评分客户端。
 */
@Component
public class AiGradingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiGradingClient.class);
    private static final String GRADE_PATH = "/internal/v1/agent/answer/grade";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final AiServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * 创建 AI 评分客户端。
     *
     * @param properties AI 服务配置
     * @param objectMapper JSON 序列化器
     */
    public AiGradingClient(AiServiceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())))
                .build();
    }

    /**
     * 调用 AI 服务评分，失败时返回空。
     *
     * @param request 评分请求
     * @return AI 评分结果
     */
    public Optional<GradingResult> grade(AiGradeRequest request) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            String body = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl() + GRADE_PATH))
                    .timeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .header(INTERNAL_TOKEN_HEADER, properties.getToken())
                    .POST(HttpRequest.BodyPublishers.ofString(body, java.nio.charset.StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                LOGGER.warn("AI 服务评分失败，HTTP状态：{}", response.statusCode());
                return Optional.empty();
            }
            return parseResponse(response.body());
        } catch (IOException | InterruptedException | IllegalArgumentException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.warn("AI 服务评分不可用，已降级本地评分，错误类型：{}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /**
     * 判断 AI 服务是否具备调用条件。
     *
     * @return 是否可调用
     */
    private boolean isAvailable() {
        return properties.isEnabled()
                && StringUtils.hasText(properties.getBaseUrl())
                && StringUtils.hasText(properties.getToken());
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
     * 解析 AI 服务统一响应。
     *
     * @param body 响应体
     * @return 评分结果
     */
    private Optional<GradingResult> parseResponse(String body) throws JsonProcessingException {
        AiApiResponse<AiGradeData> response = objectMapper.readValue(body, new TypeReference<>() { });
        if (response == null || !"SUCCESS".equals(response.code()) || response.data() == null) {
            return Optional.empty();
        }
        AiGradeData data = response.data();
        int score = Math.max(0, Math.min(100, data.score()));
        return Optional.of(new GradingResult(
                score,
                data.isCorrect(),
                safeList(data.hitPoints()),
                safeList(data.missingPoints()),
                safeList(data.problems()),
                data.referenceAnswer(),
                data.improvementAdvice(),
                safeList(data.reviewKnowledgePoints())
        ));
    }

    /**
     * 规整列表，避免空指针。
     *
     * @param values 原始列表
     * @return 安全列表
     */
    private List<String> safeList(List<String> values) {
        return values == null ? Collections.emptyList() : values;
    }

    /**
     * AI 评分请求。
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @param questionContent 题目内容
     * @param standardAnswer 标准答案
     * @param userAnswer 用户答案
     * @param knowledgePoints 知识点
     * @param contextSnippets 检索片段
     */
    public record AiGradeRequest(
            String userId,
            String questionId,
            String questionContent,
            String standardAnswer,
            String userAnswer,
            List<String> knowledgePoints,
            List<String> contextSnippets
    ) {
    }

    /** AI 服务统一响应。 */
    private record AiApiResponse<T>(String code, String message, T data, String traceId) {
    }

    /** AI 评分数据。 */
    private record AiGradeData(
            int score,
            boolean isCorrect,
            List<String> hitPoints,
            List<String> missingPoints,
            List<String> problems,
            String referenceAnswer,
            String improvementAdvice,
            List<String> reviewKnowledgePoints
    ) {
    }
}
