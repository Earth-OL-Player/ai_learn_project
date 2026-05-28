package com.earth.online.player.ailearn.common.logging;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import java.util.Arrays;

/**
 * 可在管理中心动态调整日志级别的服务。
 */
public enum ManagedLogService {

    /** Java 后端服务。 */
    BACKEND("backend", "Java 后端"),

    /** Python AI 服务。 */
    AI_SERVICE("ai-service", "Python AI 服务");

    private final String code;
    private final String label;

    /**
     * 创建日志服务枚举。
     *
     * @param code 服务编码
     * @param label 服务名称
     */
    ManagedLogService(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 获取服务编码。
     *
     * @return 服务编码
     */
    public String code() {
        return code;
    }

    /**
     * 获取服务名称。
     *
     * @return 服务名称
     */
    public String label() {
        return label;
    }

    /**
     * 根据服务编码解析枚举。
     *
     * @param code 服务编码
     * @return 日志服务枚举
     */
    public static ManagedLogService resolve(String code) {
        return Arrays.stream(values())
                .filter(service -> service.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResponseCode.PARAM_INVALID.code(), "日志服务不支持"));
    }
}
