package com.earth.online.player.ailearn.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 助手预算与数据保留配置。 */
@ConfigurationProperties(prefix = "app.assistant")
public record AssistantProperties(
        @DefaultValue("7") int sessionRetentionDays,
        @DefaultValue("30") int operationRetentionDays,
        @DefaultValue("10") int confirmationTtlMinutes,
        @DefaultValue("8") int maxToolCalls,
        @DefaultValue("120") int runTimeoutSeconds) {
}
