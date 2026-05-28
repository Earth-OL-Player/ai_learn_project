package com.earth.online.player.ailearn.model.application;

import com.earth.online.player.ailearn.common.cache.CacheInvalidationSupport;
import com.earth.online.player.ailearn.common.cache.LocalTtlCache;
import com.earth.online.player.ailearn.model.infrastructure.ModelConfigRecord;
import com.earth.online.player.ailearn.model.infrastructure.ModelEntitlementMapper;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 模型配置本地短 TTL 缓存。
 */
@Service
public class ModelConfigCache {

    private static final String ALL_CONFIGS_KEY = "ALL";
    private static final Duration MODEL_CONFIG_TTL = Duration.ofMinutes(1);

    private final ModelEntitlementMapper modelEntitlementMapper;
    private final LocalTtlCache<String, List<ModelConfigRecord>> allModelConfigsCache = new LocalTtlCache<>(MODEL_CONFIG_TTL);
    private final LocalTtlCache<String, Optional<ModelConfigRecord>> modelConfigCache = new LocalTtlCache<>(MODEL_CONFIG_TTL);

    /**
     * 创建模型配置缓存服务。
     *
     * @param modelEntitlementMapper 模型权益仓储
     */
    public ModelConfigCache(ModelEntitlementMapper modelEntitlementMapper) {
        this.modelEntitlementMapper = modelEntitlementMapper;
    }

    /**
     * 查询全部模型配置。
     *
     * @return 模型配置列表
     */
    public List<ModelConfigRecord> findAllModelConfigs() {
        return allModelConfigsCache
                .get(ALL_CONFIGS_KEY, this::loadAllModelConfigs)
                .stream()
                .map(ModelConfigCache::copyRecord)
                .toList();
    }

    /**
     * 按等级查询模型配置。
     *
     * @param modelLevel 模型等级
     * @return 模型配置
     */
    public ModelConfigRecord findModelConfig(String modelLevel) {
        return modelConfigCache
                .get(modelLevel, () -> Optional.ofNullable(copyRecord(modelEntitlementMapper.findModelConfig(modelLevel))))
                .map(ModelConfigCache::copyRecord)
                .orElse(null);
    }

    /**
     * 在事务提交后失效模型配置缓存。
     *
     * @param modelLevel 模型等级
     */
    public void invalidateAfterCommit(String modelLevel) {
        CacheInvalidationSupport.afterCommit(() -> {
            modelConfigCache.invalidate(modelLevel);
            allModelConfigsCache.clear();
        });
    }

    /**
     * 从数据库加载全部模型配置。
     *
     * @return 模型配置列表
     */
    private List<ModelConfigRecord> loadAllModelConfigs() {
        return modelEntitlementMapper.findAllModelConfigs()
                .stream()
                .map(ModelConfigCache::copyRecord)
                .toList();
    }

    /**
     * 复制模型配置记录，避免调用方修改缓存内对象。
     *
     * @param source 原始记录
     * @return 复制记录
     */
    private static ModelConfigRecord copyRecord(ModelConfigRecord source) {
        if (source == null) {
            return null;
        }
        ModelConfigRecord target = new ModelConfigRecord();
        target.setId(source.getId());
        target.setModelLevel(source.getModelLevel());
        target.setModelName(source.getModelName());
        target.setBaseUrl(source.getBaseUrl());
        target.setApiKey(source.getApiKey());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }
}
