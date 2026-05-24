package com.earth.online.player.ailearn.user.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.user.domain.User;
import com.earth.online.player.ailearn.user.domain.UserGender;
import com.earth.online.player.ailearn.user.domain.UserSummary;
import com.earth.online.player.ailearn.user.domain.UserSummaryConverter;
import com.earth.online.player.ailearn.user.infrastructure.UserMapper;
import com.earth.online.player.ailearn.user.interfaces.UpdateProfileRequest;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户应用服务。
 */
@Service
public class UserService {

    private final UserMapper userMapper;

    /**
     * 创建用户应用服务。
     *
     * @param userMapper 用户仓储
     */
    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 查询当前登录用户摘要。
     *
     * @return 当前用户摘要
     */
    public UserSummary getCurrentUser() {
        User user = findCurrentUser();
        return UserSummaryConverter.toSummary(user);
    }

    /**
     * 更新当前登录用户资料。
     *
     * @param request 用户资料请求
     * @return 更新后的当前用户摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public UserSummary updateCurrentProfile(UpdateProfileRequest request) {
        if (request == null) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "用户资料不能为空");
        }

        User user = findCurrentUser();
        String nickname = normalizeNickname(request.nickname());
        String gender = normalizeGender(request.gender());
        String motto = normalizeMotto(request.motto());
        validateNicknameUnique(nickname, user.getId());

        // 用户资料只允许本人维护基础展示字段，成长字段仍由刷题链路刷新。
        int affected = userMapper.updateProfile(user.getId(), nickname, gender, motto);
        if (affected == 0) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "用户不存在或已删除");
        }
        user.setNickname(nickname);
        user.setGender(gender);
        user.setMotto(motto);
        return UserSummaryConverter.toSummary(user);
    }

    /**
     * 查询当前登录用户。
     *
     * @return 当前用户
     */
    private User findCurrentUser() {
        AuthenticatedUser authenticatedUser = AuthContext.getUser();
        if (authenticatedUser == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录后即可使用该功能");
        }

        // 每次读取数据库，保证前端拿到最新用户展示信息。
        User user = userMapper.findById(authenticatedUser.userId());
        if (user == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录状态已失效，请重新登录");
        }
        return user;
    }

    /**
     * 校验昵称唯一性。
     *
     * @param nickname 昵称
     * @param currentUserId 当前用户ID
     */
    private void validateNicknameUnique(String nickname, Long currentUserId) {
        User sameNicknameUser = userMapper.findByNicknameAny(nickname);
        if (sameNicknameUser != null && !sameNicknameUser.getId().equals(currentUserId)) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "昵称已被使用，请更换后重试");
        }
    }

    /**
     * 规整昵称。
     *
     * @param nickname 原始昵称
     * @return 安全昵称
     */
    private String normalizeNickname(String nickname) {
        if (!StringUtils.hasText(nickname) || nickname.trim().length() > 64) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "昵称不能为空，且不能超过64位");
        }
        return nickname.trim();
    }

    /**
     * 规整性别编码。
     *
     * @param gender 原始性别编码
     * @return 安全性别编码
     */
    private String normalizeGender(String gender) {
        if (!StringUtils.hasText(gender)) {
            return null;
        }

        // 接口允许前端传小写或带空格编码，服务端统一落库为大写稳定编码。
        String normalizedGender = gender.trim().toUpperCase(Locale.ROOT);
        if (!UserGender.supports(normalizedGender)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "性别只能选择男、女或留空");
        }
        return normalizedGender;
    }

    /**
     * 规整用户座右铭。
     *
     * @param motto 原始座右铭
     * @return 安全座右铭
     */
    private String normalizeMotto(String motto) {
        if (!StringUtils.hasText(motto)) {
            return null;
        }

        // 座右铭允许留空，填写时限制长度，避免挤压 AI 刷题页侧栏展示。
        String normalizedMotto = motto.trim();
        if (normalizedMotto.length() > 60) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "座右铭不能超过60位");
        }
        return normalizedMotto;
    }

}
