package com.earth.online.player.ailearn.model.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.model.application.ModelEntitlementService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员模型配置接口。
 */
@RestController
@RequestMapping("/api/v1/admin/model-configs")
public class AdminModelConfigController {

    private final ModelEntitlementService modelEntitlementService;

    /**
     * 创建管理员模型配置接口。
     *
     * @param modelEntitlementService 模型权益服务
     */
    public AdminModelConfigController(ModelEntitlementService modelEntitlementService) {
        this.modelEntitlementService = modelEntitlementService;
    }

    /**
     * 查询全部模型配置。
     *
     * @return 模型配置列表
     */
    @GetMapping
    public ApiResponse<List<AdminModelConfigResponse>> findAll() {
        return ApiResponse.success(modelEntitlementService.findAdminModelConfigs());
    }

    /**
     * 保存指定模型等级配置。
     *
     * @param level 模型等级
     * @param request 保存请求
     * @return 保存后配置
     */
    @PutMapping("/{level}")
    public ApiResponse<AdminModelConfigResponse> save(
            @PathVariable String level,
            @RequestBody AdminModelConfigRequest request) {
        return ApiResponse.success(modelEntitlementService.saveAdminModelConfig(level, request));
    }
}
