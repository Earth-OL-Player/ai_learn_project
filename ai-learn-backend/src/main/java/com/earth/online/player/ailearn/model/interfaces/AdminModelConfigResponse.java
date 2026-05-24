package com.earth.online.player.ailearn.model.interfaces;

import java.time.OffsetDateTime;

/**
 * 管理员模型配置响应。
 *
 * @param level 模型等级
 * @param levelText 模型等级文案
 * @param modelName 模型名称
 * @param baseUrl 模型基础地址
 * @param apiKey 模型 API Key
 * @param updatedAt 更新时间
 */
public record AdminModelConfigResponse(
        String level,
        String levelText,
        String modelName,
        String baseUrl,
        String apiKey,
        OffsetDateTime updatedAt
) {
}
