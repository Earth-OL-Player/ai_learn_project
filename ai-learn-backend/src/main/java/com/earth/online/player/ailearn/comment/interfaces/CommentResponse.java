package com.earth.online.player.ailearn.comment.interfaces;

import com.earth.online.player.ailearn.interaction.domain.AuthorSummary;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 评论响应对象。
 *
 * @param id 评论ID
 * @param content 评论内容
 * @param parentId 父评论ID
 * @param likeCount 点赞数
 * @param liked 当前用户是否已点赞
 * @param replyCount 回复数量
 * @param author 作者摘要
 * @param children 子评论列表
 * @param createdAt 创建时间
 */
public record CommentResponse(
        String id,
        String content,
        String parentId,
        Integer likeCount,
        Boolean liked,
        Integer replyCount,
        AuthorSummary author,
        List<CommentResponse> children,
        OffsetDateTime createdAt
) {
}
