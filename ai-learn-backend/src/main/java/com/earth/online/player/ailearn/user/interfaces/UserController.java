package com.earth.online.player.ailearn.user.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.user.application.UserService;
import com.earth.online.player.ailearn.user.domain.UserSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口。
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    /**
     * 创建用户接口。
     *
     * @param userService 用户应用服务
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 查询当前登录用户。
     *
     * @return 当前用户摘要
     */
    @GetMapping("/me")
    public ApiResponse<UserSummary> me() {
        return ApiResponse.success(userService.getCurrentUser());
    }
}
