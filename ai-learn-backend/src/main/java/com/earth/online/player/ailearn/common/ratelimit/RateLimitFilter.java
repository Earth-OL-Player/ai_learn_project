package com.earth.online.player.ailearn.common.ratelimit;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 核心写接口内存限流过滤器。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_RULE = "auth-login";
    private static final String REGISTER_RULE = "auth-register";
    private static final String LIKE_RULE = "interaction-like";
    private static final String COMMENT_RULE = "comment-create";
    private static final String CSV_IMPORT_RULE = "system-question-csv-import";
    private static final String AI_REQUEST_RULE = "practice-ai-request";
    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String COMMENT_PATH = "/api/v1/comments";
    private static final String SUGGESTION_PATH = "/api/v1/suggestions";
    private static final String CSV_IMPORT_PATH = "/api/v1/admin/system-questions/import";
    private static final String AI_STREAM_PATH = "/api/v1/practice/messages/stream";
    private static final String LIKE_SUFFIX = "/like";

    private final RateLimitProperties properties;
    private final InMemoryRateLimitService rateLimitService;
    private final RateLimitIdentityResolver identityResolver;
    private final ObjectMapper objectMapper;

    /**
     * 创建限流过滤器。
     *
     * @param properties 限流配置
     * @param rateLimitService 内存限流服务
     * @param identityResolver 身份解析器
     * @param objectMapper JSON 序列化器
     */
    public RateLimitFilter(
            RateLimitProperties properties,
            InMemoryRateLimitService rateLimitService,
            RateLimitIdentityResolver identityResolver,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.rateLimitService = rateLimitService;
        this.identityResolver = identityResolver;
        this.objectMapper = objectMapper;
    }

    /**
     * 对命中的核心写接口执行限流。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled() || HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            applyMatchedRule(request);
            filterChain.doFilter(request, response);
        } catch (RateLimitExceededException exception) {
            writeRateLimitResponse(response, exception.getMessage());
        }
    }

    /**
     * 按请求路径匹配并执行限流规则。
     *
     * @param request HTTP 请求
     */
    private void applyMatchedRule(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return;
        }
        if (LOGIN_PATH.equals(requestUri)) {
            applyIpRule(LOGIN_RULE, request, properties.getLoginLimit(), properties.getLoginWindowSeconds());
            return;
        }
        if (REGISTER_PATH.equals(requestUri)) {
            applyIpRule(REGISTER_RULE, request, properties.getRegisterLimit(), properties.getRegisterWindowSeconds());
            return;
        }
        applyProtectedWriteRule(request, requestUri);
    }

    /**
     * 对需要登录的写接口执行用户和 IP 双维度限流。
     *
     * @param request HTTP 请求
     * @param requestUri 请求路径
     */
    private void applyProtectedWriteRule(HttpServletRequest request, String requestUri) {
        if (COMMENT_PATH.equals(requestUri)) {
            applyIpAndUserRule(COMMENT_RULE, request, properties.getCommentLimit(), properties.getCommentWindowSeconds());
            return;
        }
        if (isLikePath(requestUri)) {
            applyIpAndUserRule(LIKE_RULE, request, properties.getLikeLimit(), properties.getLikeWindowSeconds());
            return;
        }
        if (CSV_IMPORT_PATH.equals(requestUri)) {
            applyIpAndUserRule(CSV_IMPORT_RULE, request, properties.getCsvImportLimit(), properties.getCsvImportWindowSeconds());
            return;
        }
        if (AI_STREAM_PATH.equals(requestUri)) {
            applyIpAndUserRule(AI_REQUEST_RULE, request, properties.getAiRequestLimit(), properties.getAiRequestWindowSeconds());
        }
    }

    /**
     * 判断是否为点赞接口。
     *
     * @param requestUri 请求路径
     * @return 是否点赞接口
     */
    private boolean isLikePath(String requestUri) {
        return isNestedActionPath(requestUri, COMMENT_PATH, LIKE_SUFFIX)
                || isNestedActionPath(requestUri, SUGGESTION_PATH, LIKE_SUFFIX);
    }

    /**
     * 判断是否为指定前缀下的嵌套动作路径。
     *
     * @param requestUri 请求路径
     * @param prefix 路径前缀
     * @param suffix 动作后缀
     * @return 是否命中
     */
    private boolean isNestedActionPath(String requestUri, String prefix, String suffix) {
        if (!requestUri.startsWith(prefix + "/") || !requestUri.endsWith(suffix)) {
            return false;
        }

        // 当前点赞接口结构为 /资源/{id}/like，只允许一个资源ID路径段。
        String resourceId = requestUri.substring(prefix.length() + 1, requestUri.length() - suffix.length());
        return StringUtils.hasText(resourceId) && !resourceId.contains("/");
    }

    /**
     * 执行 IP 维度限流。
     *
     * @param ruleName 规则名称
     * @param request HTTP 请求
     * @param limit 限制次数
     * @param windowSeconds 窗口秒数
     */
    private void applyIpRule(String ruleName, HttpServletRequest request, int limit, int windowSeconds) {
        rateLimitService.checkFrequency(ruleName, identityResolver.resolveIpKey(request), limit, windowSeconds);
    }

    /**
     * 执行 IP 和用户双维度限流。
     *
     * @param ruleName 规则名称
     * @param request HTTP 请求
     * @param limit 限制次数
     * @param windowSeconds 窗口秒数
     */
    private void applyIpAndUserRule(String ruleName, HttpServletRequest request, int limit, int windowSeconds) {
        applyIpRule(ruleName, request, limit, windowSeconds);
        String userKey = identityResolver.resolveUserKey();
        if (StringUtils.hasText(userKey)) {
            rateLimitService.checkFrequency(ruleName, userKey, limit, windowSeconds);
        }
    }

    /**
     * 输出限流响应。
     *
     * @param response HTTP 响应
     * @param message 错误消息
     * @throws IOException IO 异常
     */
    private void writeRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // 统一使用平台响应结构，前端可以按 RATE_LIMITED 明确识别。
        ApiResponse<Void> body = ApiResponse.failure(ResponseCode.RATE_LIMITED.code(), message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
