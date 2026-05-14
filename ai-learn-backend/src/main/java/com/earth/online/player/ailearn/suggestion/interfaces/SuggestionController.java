package com.earth.online.player.ailearn.suggestion.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.suggestion.application.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 建议接口控制器。
 */
@RestController
@RequestMapping("/api/v1/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;

    /**
     * 创建建议控制器。
     *
     * @param suggestionService 建议服务
     */
    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    /**
     * 分页查询建议。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @return 建议分页响应
     */
    @GetMapping
    public ApiResponse<PageResponse<SuggestionResponse>> findPage(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(suggestionService.findPage(pageNo, pageSize));
    }

    /**
     * 提交建议。
     *
     * @param request 提交建议请求
     * @return 新建建议信息
     */
    @PostMapping
    public ApiResponse<SuggestionResponse> create(@Valid @RequestBody CreateSuggestionRequest request) {
        return ApiResponse.success(suggestionService.create(request));
    }
}
