package com.earth.online.player.ailearn.auth.interfaces;

import com.earth.online.player.ailearn.auth.application.AuthService;
import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.security.JwtUnauthorizedException;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    /**
     * 创建认证接口。
     *
     * @param authService 认证应用服务
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册。
     *
     * @param request 注册请求
     * @return 登录凭证响应
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return 登录凭证响应
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 用户退出登录。
     *
     * @param authorization 认证请求头
     * @return 退出结果
     */
    @PostMapping("/logout")
    public ApiResponse<Boolean> logout(@RequestHeader(AUTHORIZATION_HEADER) String authorization) {
        authService.logout(resolveBearerToken(authorization));
        return ApiResponse.success(Boolean.TRUE);
    }

    /**
     * 从认证请求头中提取 Bearer token。
     *
     * @param authorization 认证请求头
     * @return 访问令牌
     */
    private String resolveBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new JwtUnauthorizedException("登录后即可使用该功能");
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
