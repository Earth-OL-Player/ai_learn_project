package com.earth.online.player.ailearn.question.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.question.application.QuestionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题库接口控制器。
 */
@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private final QuestionService questionService;

    /**
     * 创建题库控制器。
     *
     * @param questionService 题库服务
     */
    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * 分页查询题目。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param keyword 关键词
     * @param questionType 题目分类
     * @return 题目分页响应
     */
    @GetMapping
    public ApiResponse<PageResponse<QuestionListResponse>> findPage(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String questionType) {
        return ApiResponse.success(questionService.findPage(
                pageNo, pageSize, keyword, questionType));
    }

    /**
     * 查询题目分类下拉数据。
     *
     * @return 题目分类列表
     */
    @GetMapping("/types")
    public ApiResponse<List<String>> findQuestionTypes() {
        return ApiResponse.success(questionService.findQuestionTypes());
    }

    /**
     * 查询热门面经阅读文档。
     *
     * @param questionType 题目分类
     * @return 热门面经题目详情列表
     */
    @GetMapping("/interview-document")
    public ApiResponse<List<QuestionDetailResponse>> findInterviewDocument(
            @RequestParam(required = false) String questionType) {
        return ApiResponse.success(questionService.findInterviewDocument(questionType));
    }

    /**
     * 查询题目详情。
     *
     * @param id 题目ID
     * @return 题目详情
     */
    @GetMapping("/{id}")
    public ApiResponse<QuestionDetailResponse> findDetail(@PathVariable Long id) {
        return ApiResponse.success(questionService.findDetail(id));
    }
}
