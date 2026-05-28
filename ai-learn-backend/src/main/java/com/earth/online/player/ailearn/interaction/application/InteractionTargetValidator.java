package com.earth.online.player.ailearn.interaction.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import java.util.function.ToIntFunction;

/**
 * 互动目标应用层校验器。
 */
public final class InteractionTargetValidator {

    /**
     * 工具类不允许实例化。
     */
    private InteractionTargetValidator() {
    }

    /**
     * 校验互动目标是否存在。
     *
     * @param targetId 互动目标ID
     * @param targetName 互动目标名称
     * @param activeCounter 有效目标计数器
     */
    public static void ensureExists(Long targetId, String targetName, ToIntFunction<Long> activeCounter) {
        if (targetId == null || activeCounter.applyAsInt(targetId) == 0) {
            throw notFound(targetName);
        }
    }

    /**
     * 读取非空查询结果。
     *
     * @param record 查询结果
     * @param targetName 互动目标名称
     * @param <T> 查询结果类型
     * @return 非空查询结果
     */
    public static <T> T requireFound(T record, String targetName) {
        if (record == null) {
            throw notFound(targetName);
        }
        return record;
    }

    /**
     * 构造互动目标不存在异常。
     *
     * @param targetName 互动目标名称
     * @return 业务异常
     */
    private static BusinessException notFound(String targetName) {
        return new BusinessException(ResponseCode.PARAM_INVALID.code(), targetName + "不存在");
    }
}
