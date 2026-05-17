package com.earth.online.player.ailearn.comment.interfaces;

import com.earth.online.player.ailearn.comment.application.CommentService;
import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论接口控制器。
 */
@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final CommentService commentService;

    /**
     * 创建评论控制器。
     *
     * @param commentService 评论服务
     */
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 分页查询评论。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param sort 排序方式
     * @return 评论分页响应
     */
    @GetMapping
    public ApiResponse<PageResponse<CommentResponse>> findPage(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sort) {
        return ApiResponse.success(commentService.findPage(pageNo, pageSize, sort));
    }

    /**
     * 发表评论。
     *
     * @param request 发表评论请求
     * @return 新建评论信息
     */
    @PostMapping
    public ApiResponse<CommentResponse> create(@Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.success(commentService.create(request));
    }

    /**
     * 点赞或取消点赞评论。
     *
     * @param id 评论ID
     * @return 最新评论信息
     */
    @PostMapping("/{id}/like")
    public ApiResponse<CommentResponse> toggleLike(@PathVariable Long id) {
        return ApiResponse.success(commentService.toggleLike(id));
    }
}
