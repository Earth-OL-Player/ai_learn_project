package com.earth.online.player.ailearn.model.interfaces;

/**
 * 模型权益展示响应。
 *
 * @param level 当前模型等级
 * @param levelText 当前模型等级文案
 * @param modelName 当前展示模型
 * @param remainingDays 剩余天数
 * @param remainingDaysText 剩余天数文案
 * @param permanent 是否永久
 * @param authorizationVisible 是否展示授权按钮
 * @param authorizationButtonText 授权按钮文案
 * @param authorizationUrl 授权入口地址
 * @param authorizationConfigured 授权入口是否已配置
 * @param frozenTip 冻结提示
 * @param frozenProRemainingDays 冻结高级模型剩余天数
 */
public record ModelEntitlementStatusResponse(
        String level,
        String levelText,
        String modelName,
        int remainingDays,
        String remainingDaysText,
        boolean permanent,
        boolean authorizationVisible,
        String authorizationButtonText,
        String authorizationUrl,
        boolean authorizationConfigured,
        String frozenTip,
        int frozenProRemainingDays
) {
}
