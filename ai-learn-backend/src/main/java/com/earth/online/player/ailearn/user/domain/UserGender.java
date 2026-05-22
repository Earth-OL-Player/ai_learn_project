package com.earth.online.player.ailearn.user.domain;

/**
 * 用户性别编码。
 */
public enum UserGender {

    /** 男性。 */
    MALE,

    /** 女性。 */
    FEMALE;

    /**
     * 判断编码是否属于支持范围。
     *
     * @param code 性别编码
     * @return 是否支持
     */
    public static boolean supports(String code) {
        for (UserGender gender : values()) {
            if (gender.name().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
