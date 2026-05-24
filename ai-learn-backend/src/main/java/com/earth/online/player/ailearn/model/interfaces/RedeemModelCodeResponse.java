package com.earth.online.player.ailearn.model.interfaces;

/**
 * 用户兑换模型权益响应。
 *
 * @param message 兑换提示
 * @param entitlement 最新权益展示
 */
public record RedeemModelCodeResponse(String message, ModelEntitlementStatusResponse entitlement) {
}
