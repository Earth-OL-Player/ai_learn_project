package com.earth.online.player.ailearn.auth.interfaces;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求。
 *
 * @param username 用户名
 * @param password 密码
 * @param nickname 昵称
 * @param email 邮箱
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_]{3,32}$", message = "用户名仅支持3到32位字母、数字和下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需为8到64位")
        String password,

        @NotBlank(message = "昵称不能为空")
        @Size(min = 1, max = 64, message = "昵称长度需为1到64位")
        String nickname,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱不能超过128位")
        String email
) {
}
