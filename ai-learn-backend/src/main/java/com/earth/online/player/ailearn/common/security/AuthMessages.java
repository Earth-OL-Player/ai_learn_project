package com.earth.online.player.ailearn.common.security;

/**
 * 认证提示文案常量。
 */
public final class AuthMessages {

    /** 未登录提示。 */
    public static final String LOGIN_REQUIRED_MESSAGE = "登录后即可使用该功能";

    /** 登录状态失效提示。 */
    public static final String SESSION_INVALID_MESSAGE = "登录状态已失效，请重新登录";

    /** 登录状态过期提示。 */
    public static final String SESSION_EXPIRED_MESSAGE = "登录状态已过期，请重新登录";

    /**
     * 常量类不允许实例化。
     */
    private AuthMessages() {
    }
}
