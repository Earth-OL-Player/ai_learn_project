package com.earth.online.player.ailearn.admin.interfaces;

import com.earth.online.player.ailearn.admin.application.AdminUserService;
import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.response.PageResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员用户管理接口。
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 创建管理员用户管理接口。
     *
     * @param adminUserService 管理员用户服务
     */
    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * 分页查询用户。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param keyword 关键词
     * @return 分页用户
     */
    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> findPage(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminUserService.findPage(pageNo, pageSize, keyword));
    }

    /**
     * 查询用户数量限制。
     *
     * @return 用户数量限制
     */
    @GetMapping("/limit")
    public ApiResponse<UserLimitResponse> findUserLimit() {
        return ApiResponse.success(adminUserService.findUserLimit());
    }

    /**
     * 更新用户数量限制。
     *
     * @param request 限制请求
     * @return 更新后限制
     */
    @PutMapping("/limit")
    public ApiResponse<UserLimitResponse> updateUserLimit(@RequestBody UserLimitRequest request) {
        return ApiResponse.success(adminUserService.updateUserLimit(request));
    }

    /**
     * 新增用户。
     *
     * @param request 保存请求
     * @return 新用户
     */
    @PostMapping
    public ApiResponse<AdminUserResponse> create(@RequestBody AdminUserRequest request) {
        return ApiResponse.success(adminUserService.create(request));
    }

    /**
     * 更新用户。
     *
     * @param id 用户ID
     * @param request 保存请求
     * @return 更新后用户
     */
    @PutMapping("/{id}")
    public ApiResponse<AdminUserResponse> update(@PathVariable Long id, @RequestBody AdminUserRequest request) {
        return ApiResponse.success(adminUserService.update(id, request));
    }

    /**
     * 删除用户。
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success(adminUserService.delete(id));
    }
}
