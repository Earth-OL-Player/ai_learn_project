package com.earth.online.player.ailearn.common.security;

/**
 * 认证上下文。
 */
public final class AuthContext {

    private static final ThreadLocal<AuthenticatedUser> USER_HOLDER = new ThreadLocal<>();

    /**
     * 工具类不允许实例化。
     */
    private AuthContext() {
    }

    /**
     * 保存当前认证用户。
     *
     * @param user 认证用户
     */
    public static void setUser(AuthenticatedUser user) {
        USER_HOLDER.set(user);
    }

    /**
     * 获取当前认证用户。
     *
     * @return 认证用户
     */
    public static AuthenticatedUser getUser() {
        return USER_HOLDER.get();
    }

    /**
     * 清理线程上下文，避免线程复用污染。
     */
    public static void clear() {
        USER_HOLDER.remove();
    }
}
