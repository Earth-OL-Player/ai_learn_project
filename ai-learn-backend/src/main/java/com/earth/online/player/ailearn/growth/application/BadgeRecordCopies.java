package com.earth.online.player.ailearn.growth.application;

import com.earth.online.player.ailearn.growth.infrastructure.BadgeRecord;

/**
 * 徽章记录复制工具。
 */
final class BadgeRecordCopies {

    /**
     * 工具类不允许实例化。
     */
    private BadgeRecordCopies() {
    }

    /**
     * 复制完整徽章记录。
     *
     * @param source 原始记录
     * @return 复制记录
     */
    static BadgeRecord copy(BadgeRecord source) {
        BadgeRecord target = copyDefinition(source);
        target.setAcquired(source.getAcquired());
        target.setAcquiredAt(source.getAcquiredAt());
        return target;
    }

    /**
     * 复制徽章静态定义，并清空用户获得状态。
     *
     * @param source 原始记录
     * @return 静态定义复制记录
     */
    static BadgeRecord copyStaticDefinition(BadgeRecord source) {
        BadgeRecord target = copyDefinition(source);
        target.setAcquired(Boolean.FALSE);
        target.setAcquiredAt(null);
        return target;
    }

    /**
     * 复制徽章静态字段。
     *
     * @param source 原始记录
     * @return 静态字段复制记录
     */
    private static BadgeRecord copyDefinition(BadgeRecord source) {
        BadgeRecord target = new BadgeRecord();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setIcon(source.getIcon());
        target.setRuleCode(source.getRuleCode());
        return target;
    }
}
