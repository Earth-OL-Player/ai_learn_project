package com.earth.online.player.ailearn.system.application;

import com.earth.online.player.ailearn.common.cache.CacheInvalidationSupport;
import com.earth.online.player.ailearn.common.cache.LocalTtlCache;
import com.earth.online.player.ailearn.system.infrastructure.SystemSettingMapper;
import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 系统设置本地短 TTL 缓存。
 */
@Service
public class SystemSettingCache {

    private static final Duration SYSTEM_SETTING_TTL = Duration.ofMinutes(1);

    private final SystemSettingMapper systemSettingMapper;
    private final LocalTtlCache<String, Optional<String>> settingValueCache = new LocalTtlCache<>(SYSTEM_SETTING_TTL);

    /**
     * 创建系统设置缓存服务。
     *
     * @param systemSettingMapper 系统设置仓储
     */
    public SystemSettingCache(SystemSettingMapper systemSettingMapper) {
        this.systemSettingMapper = systemSettingMapper;
    }

    /**
     * 查询系统设置值。
     *
     * @param settingKey 设置键
     * @return 设置值
     */
    public String findValue(String settingKey) {
        return settingValueCache
                .get(settingKey, () -> Optional.ofNullable(systemSettingMapper.findValue(settingKey)))
                .orElse(null);
    }

    /**
     * 新增或更新系统设置，并在事务提交后失效本地缓存。
     *
     * @param settingKey 设置键
     * @param settingValue 设置值
     * @return 影响行数
     */
    public int upsertValue(String settingKey, String settingValue) {
        int affected = systemSettingMapper.upsertValue(settingKey, settingValue);

        // 管理后台保存成功后主动失效，TTL 只作为异常兜底。
        invalidateAfterCommit(settingKey);
        return affected;
    }

    /**
     * 在事务提交后失效指定设置缓存。
     *
     * @param settingKey 设置键
     */
    public void invalidateAfterCommit(String settingKey) {
        CacheInvalidationSupport.afterCommit(() -> settingValueCache.invalidate(settingKey));
    }
}
