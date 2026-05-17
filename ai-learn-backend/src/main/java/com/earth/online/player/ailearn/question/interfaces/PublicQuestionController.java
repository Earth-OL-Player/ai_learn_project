package com.earth.online.player.ailearn.question.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.question.application.QuestionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 游客题库公开接口控制器。
 */
@RestController
@RequestMapping("/api/v1/public/questions")
public class PublicQuestionController {

    private final QuestionService questionService;

    /**
     * 创建游客题库公开控制器。
     *
     * @param questionService 题库服务
     */
    public PublicQuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * 查询游客可阅读的题目分类。
     *
     * @return 题目分类列表
     */
    @GetMapping("/types")
    public ApiResponse<List<String>> findQuestionTypes() {
        // 分类同属公开阅读元数据，游客进入页面时用于默认定位第一个分类。
        return ApiResponse.success(questionService.findQuestionTypes());
    }

    /**
     * 查询游客可阅读的热门面经文档。
     *
     * @param questionType 题目分类
     * @return 热门面经题目详情列表
     */
    @GetMapping("/interview-document")
    public ApiResponse<List<QuestionDetailResponse>> findInterviewDocument(
            @RequestParam(required = false) String questionType) {
        // 热门面经属于公开阅读内容，游客接口不依赖登录态和个人刷题数据。
        return ApiResponse.success(questionService.findInterviewDocument(questionType));
    }
}
