package com.earth.online.player.ailearn.model.interfaces;

/**
 * 管理员批量生成兑换码请求。
 *
 * @param codeType 兑换码类型
 * @param quantity 生成数量
 */
public record AdminRedemptionCodeGenerateRequest(String codeType, Integer quantity) {
}
