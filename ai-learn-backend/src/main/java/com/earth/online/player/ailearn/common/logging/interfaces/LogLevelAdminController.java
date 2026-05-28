package com.earth.online.player.ailearn.common.logging.interfaces;

import com.earth.online.player.ailearn.common.logging.application.LogLevelAdminService;
import com.earth.online.player.ailearn.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员日志级别管理接口。
 */
@RestController
@RequestMapping("/api/v1/admin/log-levels")
public class LogLevelAdminController {

    private final LogLevelAdminService logLevelAdminService;

    /**
     * 创建日志级别管理接口。
     *
     * @param logLevelAdminService 日志级别管理服务
     */
    public LogLevelAdminController(LogLevelAdminService logLevelAdminService) {
        this.logLevelAdminService = logLevelAdminService;
    }

    /**
     * 查询全部可管理服务日志级别。
     *
     * @return 日志级别列表
     */
    @GetMapping
    public ApiResponse<List<LogLevelResponse>> findAll() {
        return ApiResponse.success(logLevelAdminService.findAll());
    }

    /**
     * 更新指定服务日志级别。
     *
     * @param service 服务编码
     * @param request 日志级别请求
     * @return 更新后日志级别
     */
    @PutMapping("/{service}")
    public ApiResponse<LogLevelResponse> update(@PathVariable String service, @RequestBody LogLevelRequest request) {
        return ApiResponse.success(logLevelAdminService.update(service, request));
    }
}
