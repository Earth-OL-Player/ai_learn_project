package com.earth.online.player.ailearn.learning.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.learning.application.LearningRoadmapQueryService;
import com.earth.online.player.ailearn.learning.dto.LearningRoadmapResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习路线查询接口。
 */
@RestController
@RequestMapping("/api/v1/learning")
public class LearningRoadmapController {

    private final LearningRoadmapQueryService learningRoadmapQueryService;

    /**
     * 创建学习路线查询接口。
     *
     * @param learningRoadmapQueryService 学习路线查询服务
     */
    public LearningRoadmapController(LearningRoadmapQueryService learningRoadmapQueryService) {
        this.learningRoadmapQueryService = learningRoadmapQueryService;
    }

    /**
     * 查询 AI 应用开发学习路线。
     *
     * @return 学习路线统一响应
     */
    @GetMapping("/roadmap")
    public ApiResponse<LearningRoadmapResponse> roadmap() {
        return ApiResponse.success(learningRoadmapQueryService.queryRoadmap());
    }
}
