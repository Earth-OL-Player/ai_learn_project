package com.earth.online.player.ailearn.comment.interfaces;

import com.earth.online.player.ailearn.interaction.domain.InteractionTextPolicy;
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
        @Size(
                min = InteractionTextPolicy.MIN_CONTENT_LENGTH,
                max = InteractionTextPolicy.MAX_CONTENT_LENGTH,
                message = "评论内容长度需在" + InteractionTextPolicy.CONTENT_LENGTH_RANGE_TEXT + "之间"
        )
        String content,
        Long parentId
) {
}
