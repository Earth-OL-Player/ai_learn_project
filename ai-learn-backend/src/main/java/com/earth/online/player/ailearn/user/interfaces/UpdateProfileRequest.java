package com.earth.online.player.ailearn.user.interfaces;

import com.earth.online.player.ailearn.user.application.UserProfileValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 当前用户资料保存请求。
 *
 * @param nickname 昵称
 * @param gender 性别编码
 * @param motto 用户座右铭
 */
public record UpdateProfileRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(max = UserProfileValidator.MAX_NICKNAME_LENGTH, message = "昵称不能超过64位")
        String nickname,

        @Size(max = UserProfileValidator.MAX_GENDER_CODE_LENGTH, message = "性别编码不能超过16位")
        String gender,

        @Size(max = UserProfileValidator.MAX_MOTTO_LENGTH, message = "座右铭不能超过60位")
        String motto
) {
}
