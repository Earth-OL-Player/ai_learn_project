package com.earth.online.player.ailearn.common.response;

/**
 * 通用响应编码。
 */
public enum ResponseCode {

    /** 操作成功。 */
    SUCCESS("SUCCESS"),

    /** 系统内部异常。 */
    SYSTEM_ERROR("SYSTEM_ERROR"),

    /** 请求参数或业务处理失败。 */
    BUSINESS_ERROR("BUSINESS_ERROR");

    private final String code;

    /**
     * 创建响应编码。
     *
     * @param code 编码值
     */
    ResponseCode(String code) {
        this.code = code;
    }

    /**
     * 获取编码值。
     *
     * @return 编码值
     */
    public String code() {
        return code;
    }
}
