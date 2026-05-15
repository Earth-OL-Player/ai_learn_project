package com.earth.online.player.ailearn.growth.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.growth.application.GrowthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成长信息接口。
 */
@RestController
@RequestMapping("/api/v1/growth")
public class GrowthController {

    private final GrowthService growthService;

    /**
     * 创建成长信息接口。
     *
     * @param growthService 成长信息服务
     */
    public GrowthController(GrowthService growthService) {
        this.growthService = growthService;
    }

    /**
     * 查询我的成长信息。
     *
     * @return 成长信息
     */
    @GetMapping("/me")
    public ApiResponse<GrowthResponse> getMine() {
        return ApiResponse.success(growthService.getCurrentGrowth());
    }
}
