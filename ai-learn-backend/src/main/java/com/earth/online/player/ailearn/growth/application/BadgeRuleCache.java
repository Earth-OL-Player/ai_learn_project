package com.earth.online.player.ailearn.growth.application;

import com.earth.online.player.ailearn.common.cache.LocalTtlCache;
import com.earth.online.player.ailearn.growth.infrastructure.BadgeRecord;
import com.earth.online.player.ailearn.growth.infrastructure.GrowthMapper;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 徽章静态规则本地短 TTL 缓存。
 */
@Service
public class BadgeRuleCache {

    private static final String BADGE_RULES_KEY = "BADGE_RULES";
    private static final Duration BADGE_RULE_TTL = Duration.ofMinutes(10);

    private final GrowthMapper growthMapper;
    private final LocalTtlCache<String, List<BadgeRecord>> badgeRulesCache = new LocalTtlCache<>(BADGE_RULE_TTL);

    /**
     * 创建徽章静态规则缓存服务。
     *
     * @param growthMapper 成长仓储
     */
    public BadgeRuleCache(GrowthMapper growthMapper) {
        this.growthMapper = growthMapper;
    }

    /**
     * 查询全部徽章静态定义。
     *
     * @return 徽章静态定义
     */
    public List<BadgeRecord> findAllBadges() {
        return badgeRulesCache
                .get(BADGE_RULES_KEY, this::loadAllBadges)
                .stream()
                .map(BadgeRecordCopies::copyStaticDefinition)
                .toList();
    }

    /**
     * 从数据库加载徽章静态定义。
     *
     * @return 徽章静态定义
     */
    private List<BadgeRecord> loadAllBadges() {
        return growthMapper.findAllBadges()
                .stream()
                .map(BadgeRecordCopies::copyStaticDefinition)
                .toList();
    }
}
