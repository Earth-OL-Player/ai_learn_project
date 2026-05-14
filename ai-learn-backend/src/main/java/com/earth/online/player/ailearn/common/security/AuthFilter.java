package com.earth.online.player.ailearn.common.security;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 认证过滤器。
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    /** 自动续期 token 响应头。 */
    public static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;
    private final List<String> protectedPaths = List.of("/api/v1/users/me", "/api/v1/auth/logout");

    /**
     * 创建认证过滤器。
     *
     * @param jwtTokenService JWT 服务
     * @param objectMapper JSON 序列化器
     */
    public AuthFilter(JwtTokenService jwtTokenService, ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
    }

    /**
     * 对受保护接口执行 Bearer token 校验。
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
        try {
            if (isCorsPreflight(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 受保护接口必须认证，公开接口携带有效 token 时也顺便续期。
            if (isProtectedPath(request.getRequestURI())) {
                JwtParseResult parseResult = jwtTokenService.parseTokenDetail(resolveToken(request));
                AuthContext.setUser(parseResult.user());
                refreshToken(response, parseResult.user());
            } else if (hasBearerToken(request)) {
                refreshPublicRequestToken(request, response);
            }
            filterChain.doFilter(request, response);
        } catch (JwtUnauthorizedException exception) {
            writeUnauthorizedResponse(response, exception.getMessage());
        } finally {
            AuthContext.clear();
        }
    }

    /**
     * 判断是否为浏览器跨域预检请求。
     *
     * @param request HTTP 请求
     * @return 是否为预检请求
     */
    private boolean isCorsPreflight(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod());
    }

    /**
     * 认证通过后为活跃用户签发新 token。
     *
     * @param response HTTP 响应
     * @param user 认证用户
     */
    private void refreshToken(HttpServletResponse response, AuthenticatedUser user) {
        String refreshedToken = jwtTokenService.generateToken(user.userId(), user.username());
        response.setHeader(REFRESH_TOKEN_HEADER, refreshedToken);
    }

    /**
     * 公开接口携带有效 token 时刷新登录态，token 无效不影响公开接口访问。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     */
    private void refreshPublicRequestToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            JwtParseResult parseResult = jwtTokenService.parseTokenDetail(resolveToken(request));
            refreshToken(response, parseResult.user());
        } catch (JwtUnauthorizedException exception) {
            // 公开接口不因过期 token 失败，受保护接口仍会严格拦截。
        }
    }

    /**
     * 判断请求是否携带 Bearer token。
     *
     * @param request HTTP 请求
     * @return 是否携带 Bearer token
     */
    private boolean hasBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        return StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX);
    }

    /**
     * 判断是否是受保护接口。
     *
     * @param requestUri 请求路径
     * @return 是否受保护
     */
    private boolean isProtectedPath(String requestUri) {
        return protectedPaths.stream().anyMatch(requestUri::equals);
    }

    /**
     * 从请求头解析 Bearer token。
     *
     * @param request HTTP 请求
     * @return token 字符串
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new JwtUnauthorizedException("登录后即可使用该功能");
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * 输出统一认证失败响应。
     *
     * @param response HTTP 响应
     * @param message 错误消息
     * @throws IOException IO 异常
     */
    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.failure(ResponseCode.AUTH_UNAUTHORIZED.code(), message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
