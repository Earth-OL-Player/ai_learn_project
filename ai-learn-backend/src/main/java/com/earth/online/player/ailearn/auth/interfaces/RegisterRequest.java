package com.earth.online.player.ailearn.auth.interfaces;

import com.earth.online.player.ailearn.user.application.UserProfileValidator;
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
        @Pattern(regexp = UserProfileValidator.USERNAME_PATTERN_TEXT, message = UserProfileValidator.USERNAME_INVALID_MESSAGE)
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(
                min = UserProfileValidator.MIN_PASSWORD_LENGTH,
                max = UserProfileValidator.MAX_PASSWORD_LENGTH,
                message = UserProfileValidator.PASSWORD_INVALID_MESSAGE
        )
        String password,

        @NotBlank(message = "昵称不能为空")
        @Size(min = 1, max = UserProfileValidator.MAX_NICKNAME_LENGTH, message = "昵称长度需为1到64位")
        String nickname,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = UserProfileValidator.EMAIL_INVALID_MESSAGE)
        @Size(max = UserProfileValidator.MAX_EMAIL_LENGTH, message = "邮箱不能超过128位")
        String email
) {
}
