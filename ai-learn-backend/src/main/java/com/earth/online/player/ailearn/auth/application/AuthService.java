package com.earth.online.player.ailearn.auth.application;

import com.earth.online.player.ailearn.auth.interfaces.AuthResponse;
import com.earth.online.player.ailearn.auth.interfaces.LoginRequest;
import com.earth.online.player.ailearn.auth.interfaces.RegisterRequest;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.JwtTokenService;
import com.earth.online.player.ailearn.user.domain.User;
import com.earth.online.player.ailearn.user.domain.UserSummary;
import com.earth.online.player.ailearn.user.domain.UserSummaryConverter;
import com.earth.online.player.ailearn.user.infrastructure.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 认证应用服务。
 */
@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final int DEFAULT_EXPERIENCE = 0;
    private static final String DEFAULT_LEVEL_CODE = "LV1";
    private static final String DEFAULT_RANK_CODE = "BRONZE";
    private static final boolean DEFAULT_SUPER_ADMIN = false;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    /**
     * 创建认证应用服务。
     *
     * @param userMapper 用户仓储
     * @param passwordEncoder 密码编码器
     * @param jwtTokenService JWT 服务
     */
    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * 注册用户并签发令牌。
     *
     * @param request 注册请求
     * @return 登录凭证响应
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String nickname = request.nickname().trim();
        String email = request.email().trim();
        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "用户名已存在，请更换后重试");
        }
        if (userMapper.findByNickname(nickname) != null) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "昵称已被使用，请更换后重试");
        }
        if (userMapper.findByEmail(email) != null) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "邮箱已被使用，请更换后重试");
        }

        // 新用户默认写入成长体系占位数据，便于个人中心直接展示。
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setExperience(DEFAULT_EXPERIENCE);
        user.setLevelCode(DEFAULT_LEVEL_CODE);
        user.setRankCode(DEFAULT_RANK_CODE);
        user.setSuperAdmin(DEFAULT_SUPER_ADMIN);
        userMapper.insert(user);

        User savedUser = userMapper.findById(user.getId());
        return buildAuthResponse(savedUser);
    }

    /**
     * 用户登录并签发令牌。
     *
     * @param request 登录请求
     * @return 登录凭证响应
     */
    public AuthResponse login(LoginRequest request) {
        User user = userMapper.findByUsername(request.username().trim());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "用户名或密码错误");
        }
        return buildAuthResponse(user);
    }

    /**
     * 构造认证响应。
     *
     * @param user 用户信息
     * @return 认证响应
     */
    private AuthResponse buildAuthResponse(User user) {
        UserSummary summary = UserSummaryConverter.toSummary(user);
        String accessToken = jwtTokenService.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(accessToken, TOKEN_TYPE, jwtTokenService.getExpiresInSeconds(), summary);
    }

}

