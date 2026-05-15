package com.earth.online.player.ailearn.question.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.question.application.MyQuestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 我的题库接口。
 */
@RestController
@RequestMapping("/api/v1/my-questions")
public class MyQuestionController {

    private final MyQuestionService myQuestionService;

    /**
     * 创建我的题库接口。
     *
     * @param myQuestionService 我的题库服务
     */
    public MyQuestionController(MyQuestionService myQuestionService) {
        this.myQuestionService = myQuestionService;
    }

    /**
     * 分页查询我的题库。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param keyword 关键词
     * @param difficulty 难度
     * @param questionType 题型
     * @return 分页题目
     */
    @GetMapping
    public ApiResponse<PageResponse<QuestionDetailResponse>> findPage(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String questionType) {
        return ApiResponse.success(myQuestionService.findPage(pageNo, pageSize, keyword, difficulty, questionType));
    }

    /**
     * 新增个人题目。
     *
     * @param request 新增请求
     * @return 题目详情
     */
    @PostMapping
    public ApiResponse<QuestionDetailResponse> create(@Valid @RequestBody MyQuestionRequest request) {
        return ApiResponse.success(myQuestionService.create(request));
    }

    /**
     * 批量导入个人题库。
     *
     * @param request 导入请求
     * @return 导入结果
     */
    @PostMapping("/import")
    public ApiResponse<ImportMyQuestionsResponse> importQuestions(
            @Valid @RequestBody ImportMyQuestionsRequest request) {
        return ApiResponse.success(myQuestionService.importQuestions(request));
    }

    /**
     * 删除个人题目。
     *
     * @param id 题目ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteMine(@PathVariable Long id) {
        return ApiResponse.success(myQuestionService.deleteMine(id));
    }
}
