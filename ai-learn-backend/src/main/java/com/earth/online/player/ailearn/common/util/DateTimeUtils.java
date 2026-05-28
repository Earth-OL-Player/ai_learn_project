package com.earth.online.player.ailearn.common.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 日期时间转换工具。
 */
public final class DateTimeUtils {

    /**
     * 工具类不允许实例化。
     */
    private DateTimeUtils() {
    }

    /**
     * 按系统默认时区转换本地时间。
     *
     * @param value 本地时间
     * @return 带偏移时间
     */
    public static OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    /**
     * 获取系统默认时区下的当前偏移时间。
     *
     * @return 当前偏移时间
     */
    public static OffsetDateTime currentOffsetDateTime() {
        return OffsetDateTime.now(ZoneId.systemDefault());
    }
}
