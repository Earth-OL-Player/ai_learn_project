package com.earth.online.player.ailearn.admin.interfaces;

/**
 * 管理员保存用户请求。
 *
 * @param username 用户名
 * @param password 密码，更新时为空表示不修改
 * @param nickname 昵称
 * @param email 邮箱
 * @param avatar 头像地址
 * @param superAdmin 是否超级管理员
 */
public record AdminUserRequest(
        String username,
        String password,
        String nickname,
        String email,
        String avatar,
        Boolean superAdmin
) {
}
