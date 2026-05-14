package com.earth.online.player.ailearn.comment.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发表评论请求。
 *
 * @param content 评论内容
 * @param parentId 父评论ID
 */
public record CreateCommentRequest(
        @NotBlank(message = "评论内容不能为空")
        @Size(min = 2, max = 1000, message = "评论内容长度需在2到1000位之间")
        String content,
        Long parentId
) {
}
