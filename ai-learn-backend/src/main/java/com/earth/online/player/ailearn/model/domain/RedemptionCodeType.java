package com.earth.online.player.ailearn.model.domain;

import java.util.Locale;

/**
 * 模型权益兑换码类型。
 */
public enum RedemptionCodeType {

    PRO_MONTHLY("高级模型一个月", "PRO", ModelLevel.PRO, ModelEntitlementKind.MONTHLY),
    SUPER_MONTHLY("超级模型一个月", "SUPER", ModelLevel.SUPER, ModelEntitlementKind.MONTHLY),
    PRO_PERMANENT("高级模型永久", "PRO", ModelLevel.PRO, ModelEntitlementKind.PERMANENT),
    SUPER_PERMANENT("超级模型永久", "SUPER", ModelLevel.SUPER, ModelEntitlementKind.PERMANENT),
    PRO_PERMANENT_TO_SUPER("高级模型永久升超", "SUPER", ModelLevel.SUPER, ModelEntitlementKind.PERMANENT);

    private final String label;
    private final String prefix;
    private final ModelLevel targetLevel;
    private final ModelEntitlementKind entitlementKind;

    /**
     * 创建兑换码类型。
     *
     * @param label 中文名称
     * @param prefix 兑换码前缀
     * @param targetLevel 目标模型等级
     * @param entitlementKind 权益类型
     */
    RedemptionCodeType(String label, String prefix, ModelLevel targetLevel, ModelEntitlementKind entitlementKind) {
        this.label = label;
        this.prefix = prefix;
        this.targetLevel = targetLevel;
        this.entitlementKind = entitlementKind;
    }

    /**
     * 解析兑换码类型。
     *
     * @param value 原始编码
     * @return 兑换码类型
     */
    public static RedemptionCodeType resolve(String value) {
        return RedemptionCodeType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * 获取中文名称。
     *
     * @return 中文名称
     */
    public String label() {
        return label;
    }

    /**
     * 获取兑换码前缀。
     *
     * @return 兑换码前缀
     */
    public String prefix() {
        return prefix;
    }

    /**
     * 获取目标模型等级。
     *
     * @return 目标模型等级
     */
    public ModelLevel targetLevel() {
        return targetLevel;
    }

    /**
     * 获取权益类型。
     *
     * @return 权益类型
     */
    public ModelEntitlementKind entitlementKind() {
        return entitlementKind;
    }
}
