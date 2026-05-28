package com.earth.online.player.ailearn.auth.domain;

/**
 * 认证业务常量。
 */
public final class AuthConstants {

    /** 新用户默认非超级管理员。 */
    public static final boolean DEFAULT_SUPER_ADMIN = false;

    /** 默认允许注册的最大用户数。 */
    public static final int DEFAULT_MAX_USERS = 10000;

    /** 系统设置中的最大用户数键。 */
    public static final String MAX_USERS_SETTING_KEY = "MAX_USERS";

    /** 用户容量达到上限时的提示。 */
    public static final String USER_LIMIT_REACHED_MESSAGE = "当前系统用户数量已达上限，等待管理员升级服务器并扩容";

    private AuthConstants() {
    }
}
