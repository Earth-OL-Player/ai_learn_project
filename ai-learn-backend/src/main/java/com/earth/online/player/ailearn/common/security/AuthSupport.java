package com.earth.online.player.ailearn.common.security;

import com.earth.online.player.ailearn.common.constant.AppConstants;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import org.springframework.util.StringUtils;

/**
 * 认证上下文与 Bearer token 工具。
 */
public final class AuthSupport {

    /** HTTP 认证请求头。 */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    private AuthSupport() {
    }

    /**
     * 获取当前登录用户，未登录时抛出统一业务异常。
     *
     * @return 当前登录用户
     */
    public static AuthenticatedUser requireCurrentUser() {
        AuthenticatedUser currentUser = AuthContext.getUser();
        if (currentUser == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), AuthMessages.LOGIN_REQUIRED_MESSAGE);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户ID，未登录时抛出统一业务异常。
     *
     * @return 当前用户ID
     */
    public static Long requireCurrentUserId() {
        return requireCurrentUser().userId();
    }

    /**
     * 获取只读查看者用户ID，游客返回匿名占位ID。
     *
     * @return 查看者用户ID
     */
    public static long resolveViewerUserId() {
        AuthenticatedUser currentUser = AuthContext.getUser();
        return currentUser == null ? AppConstants.ANONYMOUS_USER_ID : currentUser.userId();
    }

    /**
     * 判断认证请求头是否携带 Bearer token。
     *
     * @param authorization 认证请求头
     * @return 是否携带 Bearer token
     */
    public static boolean hasBearerToken(String authorization) {
        return StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX);
    }

    /**
     * 从认证请求头提取 Bearer token。
     *
     * @param authorization 认证请求头
     * @return 访问令牌
     */
    public static String resolveBearerToken(String authorization) {
        if (!hasBearerToken(authorization)) {
            throw new JwtUnauthorizedException(AuthMessages.LOGIN_REQUIRED_MESSAGE);
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
