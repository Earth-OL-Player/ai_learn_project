package com.earth.online.player.ailearn.common.response;

/**
 * 通用响应编码。
 */
public enum ResponseCode {

    /** 操作成功。 */
    SUCCESS("SUCCESS"),

    /** 系统内部异常。 */
    SYSTEM_ERROR("SYSTEM_ERROR"),

    /** 请求参数错误。 */
    PARAM_INVALID("PARAM_INVALID"),

    /** 认证失败或登录态无效。 */
    AUTH_UNAUTHORIZED("AUTH_UNAUTHORIZED"),

    /** 资源冲突，例如用户名已存在。 */
    RESOURCE_CONFLICT("RESOURCE_CONFLICT"),

    /** 资源不存在。 */
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND"),

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
