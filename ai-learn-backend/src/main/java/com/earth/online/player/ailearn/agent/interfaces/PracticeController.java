package com.earth.online.player.ailearn.agent.interfaces;

import com.earth.online.player.ailearn.agent.application.PracticeService;
import com.earth.online.player.ailearn.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI智能刷题接口。
 */
@RestController
@RequestMapping("/api/v1/agent/practice")
public class PracticeController {

    private final PracticeService practiceService;

    /**
     * 创建刷题接口。
     *
     * @param practiceService 刷题应用服务
     */
    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    /**
     * 开始刷题。
     *
     * @param request 开始刷题请求
     * @return 题目响应
     */
    @PostMapping("/start")
    public ApiResponse<PracticeQuestionResponse> start(@RequestBody(required = false) StartPracticeRequest request) {
        return ApiResponse.success(practiceService.start(request));
    }

    /**
     * 提交答案。
     *
     * @param request 提交请求
     * @return 评分结果
     */
    @PostMapping("/submit")
    public ApiResponse<PracticeSubmitResponse> submit(@Valid @RequestBody SubmitPracticeRequest request) {
        return ApiResponse.success(practiceService.submit(request));
    }
}
