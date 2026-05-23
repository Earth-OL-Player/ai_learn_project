package com.earth.online.player.ailearn.auth.application;

import com.earth.online.player.ailearn.auth.domain.AuthConstants;
import com.earth.online.player.ailearn.auth.interfaces.AuthResponse;
import com.earth.online.player.ailearn.auth.interfaces.LoginRequest;
import com.earth.online.player.ailearn.auth.interfaces.RegisterRequest;
import com.earth.online.player.ailearn.common.constant.AppConstants;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.JwtTokenService;
import com.earth.online.player.ailearn.common.security.TokenInvalidationService;
import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;
import com.earth.online.player.ailearn.growth.infrastructure.GrowthMapper;
import com.earth.online.player.ailearn.system.infrastructure.SystemSettingMapper;
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

    private final UserMapper userMapper;
    private final GrowthMapper growthMapper;
    private final SystemSettingMapper systemSettingMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final TokenInvalidationService tokenInvalidationService;

    /**
     * 创建认证应用服务。
     *
     * @param userMapper 用户仓储
     * @param growthMapper 成长仓储
     * @param systemSettingMapper 系统设置仓储
     * @param passwordEncoder 密码编码器
     * @param jwtTokenService JWT 服务
     * @param tokenInvalidationService 令牌失效服务
     */
    public AuthService(
            UserMapper userMapper,
            GrowthMapper growthMapper,
            SystemSettingMapper systemSettingMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            TokenInvalidationService tokenInvalidationService) {
        this.userMapper = userMapper;
        this.growthMapper = growthMapper;
        this.systemSettingMapper = systemSettingMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.tokenInvalidationService = tokenInvalidationService;
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
        if (userMapper.findByUsernameAny(username) != null) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "用户名已存在，请更换后重试");
        }
        if (userMapper.findByNicknameAny(nickname) != null) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "昵称已被使用，请更换后重试");
        }
        if (userMapper.findByEmailAny(email) != null) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "邮箱已被使用，请更换后重试");
        }
        ensureUserCapacityAvailable();

        // 新用户默认写入成长体系占位数据，便于个人中心直接展示。
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setGender(null);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setExperience(AuthConstants.DEFAULT_EXPERIENCE);
        user.setLevelCode(AuthConstants.DEFAULT_LEVEL_CODE);
        user.setRankCode(AuthConstants.DEFAULT_RANK_CODE);
        user.setSuperAdmin(AuthConstants.DEFAULT_SUPER_ADMIN);
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
     * 退出登录并服务端失效当前令牌。
     *
     * @param token 当前访问令牌
     */
    public void logout(String token) {
        tokenInvalidationService.invalidate(jwtTokenService.parseTokenDetail(token));
    }

    /**
     * 校验系统用户容量是否仍可注册。
     */
    private void ensureUserCapacityAvailable() {
        long currentUsers = userMapper.countActiveUsers();
        if (currentUsers >= resolveMaxUsers()) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), AuthConstants.USER_LIMIT_REACHED_MESSAGE);
        }
    }

    /**
     * 读取最大用户数设置。
     *
     * @return 最大用户数
     */
    private int resolveMaxUsers() {
        String value = systemSettingMapper.findValue(AuthConstants.MAX_USERS_SETTING_KEY);
        if (value == null || value.isBlank()) {
            return AuthConstants.DEFAULT_MAX_USERS;
        }
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return AuthConstants.DEFAULT_MAX_USERS;
        }
    }

    /**
     * 构造认证响应。
     *
     * @param user 用户信息
     * @return 认证响应
     */
    private AuthResponse buildAuthResponse(User user) {
        refreshGrowthSnapshot(user);
        UserSummary summary = UserSummaryConverter.toSummary(user);
        String accessToken = jwtTokenService.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(accessToken, AppConstants.TOKEN_TYPE_BEARER, jwtTokenService.getExpiresInSeconds(), summary);
    }

    /**
     * 刷新用户成长快照。
     *
     * @param user 用户信息
     */
    private void refreshGrowthSnapshot(User user) {
        int experience = Math.max(0, growthMapper.sumBestScores(user.getId()));
        GrowthLevel level = GrowthLevel.resolveByExperience(experience);
        GrowthRank rank = GrowthRank.resolveByExperience(experience);

        // 登录响应也使用最新成长规则，避免旧等级名称继续显示。
        userMapper.updateGrowth(user.getId(), experience, level.code(), rank.code());
        user.setExperience(experience);
        user.setLevelCode(level.code());
        user.setRankCode(rank.code());
    }

}
