package com.earth.online.player.ailearn.auth.interfaces;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求。
 *
 * @param username 用户名
 * @param password 密码
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password
) {
}
