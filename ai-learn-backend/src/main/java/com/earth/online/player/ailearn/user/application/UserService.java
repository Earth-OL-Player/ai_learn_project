package com.earth.online.player.ailearn.user.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.user.domain.User;
import com.earth.online.player.ailearn.user.domain.UserSummary;
import com.earth.online.player.ailearn.user.domain.UserSummaryConverter;
import com.earth.online.player.ailearn.user.infrastructure.UserMapper;
import org.springframework.stereotype.Service;

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
        AuthenticatedUser authenticatedUser = AuthContext.getUser();
        if (authenticatedUser == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录后即可使用该功能");
        }

        // 每次读取数据库，保证前端拿到最新用户展示信息。
        User user = userMapper.findById(authenticatedUser.userId());
        if (user == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录状态已失效，请重新登录");
        }
        return UserSummaryConverter.toSummary(user);
    }
}
