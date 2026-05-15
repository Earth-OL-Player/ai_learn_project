package com.earth.online.player.ailearn.growth.interfaces;

import java.time.OffsetDateTime;

/**
 * 徽章响应对象。
 *
 * @param id 徽章ID
 * @param name 徽章名称
 * @param description 徽章说明
 * @param icon 图标
 * @param ruleCode 规则编码
 * @param acquired 是否已获得
 * @param acquiredAt 获得时间
 */
public record BadgeResponse(
        String id,
        String name,
        String description,
        String icon,
        String ruleCode,
        boolean acquired,
        OffsetDateTime acquiredAt
) {
}
