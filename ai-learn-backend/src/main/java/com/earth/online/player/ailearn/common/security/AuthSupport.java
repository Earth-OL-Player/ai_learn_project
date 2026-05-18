package com.earth.online.player.ailearn.common.security;

import com.earth.online.player.ailearn.common.constant.AppConstants;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;

/**
 * 登录用户读取工具。
 */
public final class AuthSupport {

    private static final String LOGIN_REQUIRED_MESSAGE = "登录后即可使用该功能";

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
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), LOGIN_REQUIRED_MESSAGE);
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
}
