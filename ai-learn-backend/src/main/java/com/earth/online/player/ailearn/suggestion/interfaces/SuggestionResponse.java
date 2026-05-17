package com.earth.online.player.ailearn.suggestion.interfaces;

import com.earth.online.player.ailearn.interaction.domain.AuthorSummary;
import java.time.OffsetDateTime;

/**
 * 建议响应对象。
 *
 * @param id 建议ID
 * @param content 建议内容
 * @param type 建议类型
 * @param typeText 建议类型文案
 * @param likeCount 点赞数
 * @param liked 当前用户是否已点赞
 * @param author 作者摘要
 * @param createdAt 创建时间
 */
public record SuggestionResponse(
        String id,
        String content,
        String type,
        String typeText,
        Integer likeCount,
        Boolean liked,
        AuthorSummary author,
        OffsetDateTime createdAt
) {
}
