package com.earth.online.player.ailearn.user.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * 用户资料字段校验器。
 */
public final class UserProfileValidator {

    /** 用户名正则。 */
    public static final String USERNAME_PATTERN_TEXT = "^[A-Za-z0-9_]{3,32}$";

    /** 密码最小长度。 */
    public static final int MIN_PASSWORD_LENGTH = 8;

    /** 密码最大长度。 */
    public static final int MAX_PASSWORD_LENGTH = 64;

    /** 昵称最大长度。 */
    public static final int MAX_NICKNAME_LENGTH = 64;

    /** 邮箱最大长度。 */
    public static final int MAX_EMAIL_LENGTH = 128;

    /** 性别编码最大长度。 */
    public static final int MAX_GENDER_CODE_LENGTH = 16;

    /** 座右铭最大长度。 */
    public static final int MAX_MOTTO_LENGTH = 60;

    /** 用户名不合法提示。 */
    public static final String USERNAME_INVALID_MESSAGE = "用户名仅支持3到32位字母、数字和下划线";

    /** 密码不合法提示。 */
    public static final String PASSWORD_INVALID_MESSAGE = "密码长度需为8到64位";

    /** 昵称不合法提示。 */
    public static final String NICKNAME_INVALID_MESSAGE = "昵称不能为空，且不能超过64位";

    /** 邮箱不合法提示。 */
    public static final String EMAIL_INVALID_MESSAGE = "邮箱格式不正确";

    private static final Pattern USERNAME_PATTERN = Pattern.compile(USERNAME_PATTERN_TEXT);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    /**
     * 工具类不允许实例化。
     */
    private UserProfileValidator() {
    }

    /**
     * 规整用户名。
     *
     * @param username 原始用户名
     * @return 安全用户名
     */
    public static String normalizeUsername(String username) {
        if (!StringUtils.hasText(username) || !USERNAME_PATTERN.matcher(username.trim()).matches()) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), USERNAME_INVALID_MESSAGE);
        }
        return username.trim();
    }

    /**
     * 校验密码长度。
     *
     * @param password 原始密码
     */
    public static void validatePasswordLength(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), PASSWORD_INVALID_MESSAGE);
        }
    }

    /**
     * 规整用户昵称。
     *
     * @param nickname 原始昵称
     * @return 安全昵称
     */
    public static String normalizeNickname(String nickname) {
        if (!StringUtils.hasText(nickname) || nickname.trim().length() > MAX_NICKNAME_LENGTH) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), NICKNAME_INVALID_MESSAGE);
        }
        return nickname.trim();
    }

    /**
     * 规整邮箱。
     *
     * @param email 原始邮箱
     * @return 安全邮箱
     */
    public static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)
                || email.length() > MAX_EMAIL_LENGTH
                || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), EMAIL_INVALID_MESSAGE);
        }
        return email.trim();
    }
}
