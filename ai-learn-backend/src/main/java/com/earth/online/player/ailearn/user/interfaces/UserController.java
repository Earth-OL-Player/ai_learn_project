package com.earth.online.player.ailearn.user.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.user.application.UserService;
import com.earth.online.player.ailearn.user.application.UserQuestionStatsService;
import com.earth.online.player.ailearn.user.domain.UserSummary;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口。
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserQuestionStatsService userQuestionStatsService;

    /**
     * 创建用户接口。
     *
     * @param userService 用户应用服务
     * @param userQuestionStatsService 用户刷题汇总只读服务
     */
    public UserController(UserService userService, UserQuestionStatsService userQuestionStatsService) {
        this.userService = userService;
        this.userQuestionStatsService = userQuestionStatsService;
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

    /**
     * 查询当前用户智能刷题记录。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param keyword 题目关键词
     * @param questionType 题目类型
     * @return 当前用户刷题记录分页数据
     */
    @GetMapping("/me/question-stats")
    public ApiResponse<PageResponse<UserQuestionStatsItemResponse>> questionStats(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String questionType
    ) {
        return ApiResponse.success(userQuestionStatsService.findCurrentUserStatsPage(pageNo, pageSize, keyword, questionType));
    }

    /**
     * 查询当前用户智能刷题记录概览。
     *
     * @return 当前用户刷题记录概览
     */
    @GetMapping("/me/question-stats/overview")
    public ApiResponse<UserQuestionStatsOverviewResponse> questionStatsOverview() {
        return ApiResponse.success(userQuestionStatsService.getCurrentUserStatsOverview());
    }

    /**
     * 更新当前登录用户资料。
     *
     * @param request 资料保存请求
     * @return 当前用户摘要
     */
    @PutMapping("/me/profile")
    public ApiResponse<UserSummary> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateCurrentProfile(request));
    }
}
