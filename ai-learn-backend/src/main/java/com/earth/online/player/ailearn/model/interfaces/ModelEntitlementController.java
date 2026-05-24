package com.earth.online.player.ailearn.model.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.model.application.ModelEntitlementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户模型权益接口。
 */
@RestController
@RequestMapping("/api/v1/model-entitlements")
public class ModelEntitlementController {

    private final ModelEntitlementService modelEntitlementService;

    /**
     * 创建用户模型权益接口。
     *
     * @param modelEntitlementService 模型权益服务
     */
    public ModelEntitlementController(ModelEntitlementService modelEntitlementService) {
        this.modelEntitlementService = modelEntitlementService;
    }

    /**
     * 查询当前访问者模型权益。
     *
     * @return 当前模型权益
     */
    @GetMapping("/status")
    public ApiResponse<ModelEntitlementStatusResponse> status() {
        return ApiResponse.success(modelEntitlementService.getCurrentStatus());
    }

    /**
     * 兑换模型权益兑换码。
     *
     * @param request 兑换请求
     * @return 兑换结果
     */
    @PostMapping("/redeem")
    public ApiResponse<RedeemModelCodeResponse> redeem(@RequestBody RedeemModelCodeRequest request) {
        return ApiResponse.success(modelEntitlementService.redeem(request));
    }
}
