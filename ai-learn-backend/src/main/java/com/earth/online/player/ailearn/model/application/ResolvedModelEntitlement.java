package com.earth.online.player.ailearn.model.application;

import com.earth.online.player.ailearn.model.domain.AiModelRequestConfig;
import com.earth.online.player.ailearn.model.domain.ModelLevel;

/**
 * 已解析的用户模型权益。
 *
 * @param level 当前生效等级
 * @param modelName 展示和调用的模型名称
 * @param remainingDays 剩余天数
 * @param permanent 是否永久
 * @param frozenProRemainingDays 冻结高级模型剩余天数
 * @param requestConfig AI 请求级模型配置
 */
public record ResolvedModelEntitlement(
        ModelLevel level,
        String modelName,
        int remainingDays,
        boolean permanent,
        int frozenProRemainingDays,
        AiModelRequestConfig requestConfig
) {
}
