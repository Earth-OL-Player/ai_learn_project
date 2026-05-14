package com.earth.online.player.ailearn.comment.interfaces;

import com.earth.online.player.ailearn.interaction.domain.AuthorSummary;
import java.time.OffsetDateTime;

/**
 * 评论响应对象。
 *
 * @param id 评论ID
 * @param content 评论内容
 * @param parentId 父评论ID
 * @param likeCount 点赞数
 * @param author 作者摘要
 * @param createdAt 创建时间
 */
public record CommentResponse(
        String id,
        String content,
        String parentId,
        Integer likeCount,
        AuthorSummary author,
        OffsetDateTime createdAt
) {
}
