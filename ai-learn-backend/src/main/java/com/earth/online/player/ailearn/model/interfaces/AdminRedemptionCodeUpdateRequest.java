package com.earth.online.player.ailearn.model.interfaces;

/**
 * 管理员编辑兑换码请求。
 *
 * @param codeType 兑换码类型
 */
public record AdminRedemptionCodeUpdateRequest(String codeType) {
}
