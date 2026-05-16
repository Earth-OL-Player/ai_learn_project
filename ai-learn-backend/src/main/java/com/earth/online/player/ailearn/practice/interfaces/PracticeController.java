package com.earth.online.player.ailearn.practice.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.practice.application.PracticeService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 智能刷题接口。
 */
@RestController
@RequestMapping("/api/v1/practice")
public class PracticeController {

    private final PracticeService practiceService;

    /**
     * 创建 AI 智能刷题接口。
     *
     * @param practiceService 刷题应用服务
     */
    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    /**
     * 查询题目分类。
     *
     * @return 题目分类
     */
    @GetMapping("/categories")
    public ApiResponse<List<String>> findQuestionTypes() {
        return ApiResponse.success(practiceService.findQuestionTypes());
    }

    /**
     * 查询当前刷题状态。
     *
     * @return 当前状态
     */
    @GetMapping("/state")
    public ApiResponse<PracticeStateResponse> getState() {
        return ApiResponse.success(practiceService.getState());
    }

    /**
     * 抽取下一题。
     *
     * @param request 出题请求
     * @return 出题结果
     */
    @PostMapping("/next-question")
    public ApiResponse<PracticeMessageResponse> nextQuestion(@RequestBody(required = false) PracticeActionRequest request) {
        return ApiResponse.success(practiceService.nextQuestion(request));
    }

    /**
     * 重新回答当前题。
     *
     * @return 当前题
     */
    @PostMapping("/retry")
    public ApiResponse<PracticeMessageResponse> retryCurrentQuestion() {
        return ApiResponse.success(practiceService.retryCurrentQuestion());
    }

    /**
     * 处理用户聊天输入。
     *
     * @param request 聊天请求
     * @return 聊天结果
     */
    @PostMapping("/messages")
    public ApiResponse<PracticeMessageResponse> handleMessage(@RequestBody PracticeMessageRequest request) {
        return ApiResponse.success(practiceService.handleMessage(request));
    }
}
