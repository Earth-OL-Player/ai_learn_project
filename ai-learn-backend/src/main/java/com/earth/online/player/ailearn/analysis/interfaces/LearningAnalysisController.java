package com.earth.online.player.ailearn.analysis.interfaces;

import com.earth.online.player.ailearn.analysis.application.LearningAnalysisService;
import com.earth.online.player.ailearn.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习分析接口。
 */
@RestController
@RequestMapping("/api/v1/learning-analysis")
public class LearningAnalysisController {

    private final LearningAnalysisService learningAnalysisService;

    /**
     * 创建学习分析接口。
     *
     * @param learningAnalysisService 学习分析服务
     */
    public LearningAnalysisController(LearningAnalysisService learningAnalysisService) {
        this.learningAnalysisService = learningAnalysisService;
    }

    /**
     * 查询我的学习分析。
     *
     * @return 学习分析
     */
    @GetMapping("/me")
    public ApiResponse<LearningAnalysisResponse> getMine() {
        return ApiResponse.success(learningAnalysisService.getMine());
    }
}
