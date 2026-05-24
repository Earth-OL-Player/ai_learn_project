package com.earth.online.player.ailearn.user.interfaces;

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
        @Size(max = 64, message = "昵称不能超过64位")
        String nickname,

        @Size(max = 16, message = "性别编码不能超过16位")
        String gender,

        @Size(max = 60, message = "座右铭不能超过60位")
        String motto
) {
}
