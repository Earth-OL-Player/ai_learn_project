package com.earth.online.player.ailearn.interaction.domain;

import java.util.Locale;

/**
 * 互动列表排序方式。
 */
public enum InteractionSort {

    /** 最热排序，按点赞数倒序。 */
    HOT,

    /** 最新排序，按发布时间倒序。 */
    LATEST;

    /**
     * 根据请求参数解析排序方式。
     *
     * @param value 原始排序参数
     * @return 安全排序方式
     */
    public static InteractionSort from(String value) {
        if (value == null || value.isBlank()) {
            return HOT;
        }

        // 未知排序统一回退最热，避免接口因轻微参数错误不可用。
        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        for (InteractionSort sort : values()) {
            if (sort.name().equals(normalizedValue)) {
                return sort;
            }
        }
        return HOT;
    }
}
