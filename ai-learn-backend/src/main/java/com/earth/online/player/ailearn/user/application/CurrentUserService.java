package com.earth.online.player.ailearn.user.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthMessages;
import com.earth.online.player.ailearn.common.security.AuthSupport;
import com.earth.online.player.ailearn.user.domain.User;
import com.earth.online.player.ailearn.user.infrastructure.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 当前登录用户读取服务。
 */
@Service
public class CurrentUserService {

    private final UserMapper userMapper;

    /**
     * 创建当前用户读取服务。
     *
     * @param userMapper 用户仓储
     */
    public CurrentUserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 查询当前登录用户，未登录或用户失效时抛出统一异常。
     *
     * @return 当前用户
     */
    public User requireCurrentUser() {
        Long userId = AuthSupport.requireCurrentUserId();
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), AuthMessages.SESSION_INVALID_MESSAGE);
        }
        return user;
    }

    /**
     * 查询当前超级管理员，非超级管理员时抛出指定无权限提示。
     *
     * @param forbiddenMessage 无权限提示
     * @return 当前超级管理员
     */
    public User requireSuperAdmin(String forbiddenMessage) {
        Long userId = AuthSupport.requireCurrentUserId();
        User user = userMapper.findById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getSuperAdmin())) {
            throw new BusinessException(ResponseCode.AUTH_FORBIDDEN.code(), forbiddenMessage);
        }
        return user;
    }
}
