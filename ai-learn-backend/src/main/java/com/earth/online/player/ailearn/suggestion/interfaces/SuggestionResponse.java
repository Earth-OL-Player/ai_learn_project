package com.earth.online.player.ailearn.suggestion.interfaces;

import com.earth.online.player.ailearn.interaction.domain.AuthorSummary;
import java.time.OffsetDateTime;

/**
 * 建议响应对象。
 *
 * @param id 建议ID
 * @param title 建议标题
 * @param content 建议内容
 * @param type 建议类型
 * @param typeText 建议类型文案
 * @param status 处理状态
 * @param statusText 处理状态文案
 * @param author 作者摘要
 * @param createdAt 创建时间
 */
public record SuggestionResponse(
        String id,
        String title,
        String content,
        String type,
        String typeText,
        String status,
        String statusText,
        AuthorSummary author,
        OffsetDateTime createdAt
) {
}
