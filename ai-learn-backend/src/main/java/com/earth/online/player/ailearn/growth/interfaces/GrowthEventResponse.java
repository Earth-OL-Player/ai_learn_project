package com.earth.online.player.ailearn.growth.interfaces;

import java.time.OffsetDateTime;

/**
 * 成长事件响应对象。
 *
 * @param id 事件ID
 * @param eventType 事件类型
 * @param title 标题
 * @param description 说明
 * @param experienceDelta 经验变化
 * @param createdAt 创建时间
 */
public record GrowthEventResponse(
        String id,
        String eventType,
        String title,
        String description,
        int experienceDelta,
        OffsetDateTime createdAt
) {
}
