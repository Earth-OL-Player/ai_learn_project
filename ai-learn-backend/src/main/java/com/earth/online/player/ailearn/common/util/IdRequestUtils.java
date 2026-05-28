package com.earth.online.player.ailearn.common.util;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;

/**
 * 请求 ID 校验工具。
 */
public final class IdRequestUtils {

    /**
     * 工具类不允许实例化。
     */
    private IdRequestUtils() {
    }

    /**
     * 要求 ID 必须是正数。
     *
     * @param id 原始 ID
     * @param invalidMessage 非法 ID 提示
     * @return 合法 ID
     */
    public static Long requirePositive(Long id, String invalidMessage) {
        if (id == null || id < 1) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), invalidMessage);
        }
        return id;
    }
}
