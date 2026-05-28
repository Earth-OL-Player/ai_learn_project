package com.earth.online.player.ailearn.common.logging.interfaces;

import java.time.OffsetDateTime;

/**
 * 日志级别响应。
 *
 * @param service 服务编码
 * @param serviceName 服务名称
 * @param level 当前日志级别
 * @param available 服务是否可管理
 * @param message 当前状态说明
 * @param updatedAt 读取或更新发生时间
 */
public record LogLevelResponse(
        String service,
        String serviceName,
        String level,
        boolean available,
        String message,
        OffsetDateTime updatedAt) {
}
