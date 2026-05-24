package com.earth.online.player.ailearn.model.interfaces;

import java.time.OffsetDateTime;

/**
 * 管理员兑换码响应。
 *
 * @param id 兑换码ID
 * @param code 兑换码
 * @param codeType 兑换码类型
 * @param codeTypeText 兑换码类型文案
 * @param status 状态
 * @param statusText 状态文案
 * @param usedByUserId 使用用户ID
 * @param usedByUsername 使用用户名
 * @param usedAt 使用时间
 * @param createdAt 创建时间
 * @param editable 是否可编辑
 * @param deletable 是否可删除
 */
public record AdminRedemptionCodeResponse(
        String id,
        String code,
        String codeType,
        String codeTypeText,
        String status,
        String statusText,
        String usedByUserId,
        String usedByUsername,
        OffsetDateTime usedAt,
        OffsetDateTime createdAt,
        boolean editable,
        boolean deletable
) {
}
