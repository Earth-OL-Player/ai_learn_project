package com.earth.online.player.ailearn.admin.application;

import com.earth.online.player.ailearn.admin.interfaces.AdminUserRequest;
import com.earth.online.player.ailearn.admin.interfaces.AdminUserResponse;
import com.earth.online.player.ailearn.admin.interfaces.UserLimitRequest;
import com.earth.online.player.ailearn.admin.interfaces.UserLimitResponse;
import com.earth.online.player.ailearn.auth.domain.AuthConstants;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.util.DateTimeUtils;
import com.earth.online.player.ailearn.common.util.IdRequestUtils;
import com.earth.online.player.ailearn.common.util.NumberUtils;
import com.earth.online.player.ailearn.common.util.PageRequestUtils;
import com.earth.online.player.ailearn.common.util.TextUtils;
import com.earth.online.player.ailearn.system.application.SystemSettingCache;
import com.earth.online.player.ailearn.user.application.CurrentUserService;
import com.earth.online.player.ailearn.user.application.UserDefaults;
import com.earth.online.player.ailearn.user.application.UserProfileValidator;
import com.earth.online.player.ailearn.user.domain.User;
import com.earth.online.player.ailearn.user.infrastructure.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 管理员用户管理应用服务。
 */
@Service
public class AdminUserService {

    private static final int MIN_MAX_USERS = 1;
    private static final int MAX_MAX_USERS = 1_000_000;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SystemSettingCache systemSettingCache;
    private final CurrentUserService currentUserService;

    /**
     * 创建管理员用户管理服务。
     *
     * @param userMapper 用户仓储
     * @param passwordEncoder 密码编码器
     * @param systemSettingCache 系统设置缓存
     * @param currentUserService 当前用户服务
     */
    public AdminUserService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            SystemSettingCache systemSettingCache,
            CurrentUserService currentUserService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.systemSettingCache = systemSettingCache;
        this.currentUserService = currentUserService;
    }

    /**
     * 分页查询用户。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param keyword 关键词
     * @return 用户分页
     */
    public PageResponse<AdminUserResponse> findPage(Integer pageNo, Integer pageSize, String keyword) {
        requireSuperAdmin();
        int safePageNo = PageRequestUtils.normalizePageNo(pageNo);
        int safePageSize = PageRequestUtils.normalizePageSize(pageSize);
        String safeKeyword = TextUtils.trimToNull(keyword);
        int offset = PageRequestUtils.calculateOffset(safePageNo, safePageSize);

        // 管理列表只展示未删除用户，避免误操作历史账号。
        return new PageResponse<>(
                userMapper.findAdminPage(safeKeyword, offset, safePageSize).stream().map(this::toResponse).toList(),
                safePageNo,
                safePageSize,
                userMapper.countAdminPage(safeKeyword)
        );
    }

    /**
     * 新增用户。
     *
     * @param request 用户请求
     * @return 新用户
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminUserResponse create(AdminUserRequest request) {
        requireSuperAdmin();
        User user = buildUser(request, null, true);
        userMapper.insert(user);
        return toResponse(userMapper.findById(user.getId()));
    }

    /**
     * 更新用户。
     *
     * @param id 用户ID
     * @param request 用户请求
     * @return 更新后用户
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminUserResponse update(Long id, AdminUserRequest request) {
        User operator = requireSuperAdmin();
        User existing = findExisting(id);
        User user = buildUser(request, existing, false);
        if (operator.getId().equals(id) && Boolean.FALSE.equals(user.getSuperAdmin())) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "不能取消自己的超级管理员权限");
        }

        // 管理端更新允许不传密码，避免每次编辑用户都重置密码。
        userMapper.updateByAdmin(user);
        return toResponse(userMapper.findById(id));
    }

    /**
     * 删除用户。
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        User operator = requireSuperAdmin();
        if (operator.getId().equals(id)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "不能删除当前登录管理员");
        }
        findExisting(id);
        int affected = userMapper.softDeleteById(id);
        if (affected == 0) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "用户不存在或已删除");
        }
        return true;
    }

    /**
     * 查询用户数量限制。
     *
     * @return 用户数量限制
     */
    public UserLimitResponse findUserLimit() {
        requireSuperAdmin();
        return new UserLimitResponse(resolveMaxUsers(), userMapper.countActiveUsers());
    }

    /**
     * 更新用户数量限制。
     *
     * @param request 限制请求
     * @return 更新后限制
     */
    @Transactional(rollbackFor = Exception.class)
    public UserLimitResponse updateUserLimit(UserLimitRequest request) {
        requireSuperAdmin();
        int maxUsers = normalizeMaxUsers(request == null ? null : request.maxUsers());
        systemSettingCache.upsertValue(AuthConstants.MAX_USERS_SETTING_KEY, String.valueOf(maxUsers));
        return new UserLimitResponse(maxUsers, userMapper.countActiveUsers());
    }

    /**
     * 读取系统最大用户数。
     *
     * @return 最大用户数
     */
    public int resolveMaxUsers() {
        String value = systemSettingCache.findValue(AuthConstants.MAX_USERS_SETTING_KEY);
        if (!StringUtils.hasText(value)) {
            return AuthConstants.DEFAULT_MAX_USERS;
        }
        try {
            return normalizeMaxUsers(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return AuthConstants.DEFAULT_MAX_USERS;
        }
    }

    /**
     * 校验当前用户必须是超级管理员。
     *
     * @return 当前超级管理员
     */
    private User requireSuperAdmin() {
        return currentUserService.requireSuperAdmin("仅超级管理员可维护用户");
    }

    /**
     * 查询未删除用户。
     *
     * @param id 用户ID
     * @return 用户信息
     */
    private User findExisting(Long id) {
        Long safeId = IdRequestUtils.requirePositive(id, "用户ID不合法");
        User user = userMapper.findById(safeId);
        if (user == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "用户不存在或已删除");
        }
        return user;
    }

    /**
     * 构建用户对象。
     *
     * @param request 用户请求
     * @param existing 现有用户
     * @param createMode 是否新增
     * @return 用户对象
     */
    private User buildUser(AdminUserRequest request, User existing, boolean createMode) {
        if (request == null) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "用户信息不能为空");
        }
        String username = UserProfileValidator.normalizeUsername(request.username());
        String nickname = UserProfileValidator.normalizeNickname(request.nickname());
        String email = UserProfileValidator.normalizeEmail(request.email());
        validateUniqueness(username, nickname, email, existing == null ? null : existing.getId());

        // 新增必须提供密码；编辑时密码为空表示保持原密码。
        User user = new User();
        user.setId(existing == null ? null : existing.getId());
        user.setUsername(username);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setAvatar(TextUtils.trimToNull(request.avatar()));
        user.setPasswordHash(resolvePasswordHash(request.password(), createMode));
        if (existing == null) {
            UserDefaults.applyNewUserGrowth(user);
        } else {
            user.setExperience(existing.getExperience());
            user.setLevelCode(existing.getLevelCode());
            user.setRankCode(existing.getRankCode());
        }
        user.setSuperAdmin(Boolean.TRUE.equals(request.superAdmin()));
        return user;
    }

    /**
     * 校验唯一字段。
     *
     * @param username 用户名
     * @param nickname 昵称
     * @param email 邮箱
     * @param currentId 当前用户ID
     */
    private void validateUniqueness(String username, String nickname, String email, Long currentId) {
        validateSameUser(userMapper.findByUsernameAny(username), currentId, "用户名已存在，请更换后重试");
        validateSameUser(userMapper.findByNicknameAny(nickname), currentId, "昵称已被使用，请更换后重试");
        validateSameUser(userMapper.findByEmailAny(email), currentId, "邮箱已被使用，请更换后重试");
    }

    /**
     * 校验命中的用户是否为当前编辑对象。
     *
     * @param user 命中用户
     * @param currentId 当前用户ID
     * @param message 错误提示
     */
    private void validateSameUser(User user, Long currentId, String message) {
        if (user != null && (currentId == null || !user.getId().equals(currentId))) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), message);
        }
    }

    /**
     * 处理密码哈希。
     *
     * @param password 原始密码
     * @param createMode 是否新增
     * @return 密码哈希
     */
    private String resolvePasswordHash(String password, boolean createMode) {
        if (!StringUtils.hasText(password)) {
            if (createMode) {
                throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "新增用户密码不能为空");
            }
            return null;
        }
        UserProfileValidator.validatePasswordLength(password);
        return passwordEncoder.encode(password);
    }

    /**
     * 规整最大用户数。
     *
     * @param maxUsers 原始最大用户数
     * @return 安全最大用户数
     */
    private int normalizeMaxUsers(Integer maxUsers) {
        if (maxUsers == null || maxUsers < MIN_MAX_USERS || maxUsers > MAX_MAX_USERS) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "最大用户数必须在1到1000000之间");
        }
        return maxUsers;
    }

    /**
     * 转换用户响应。
     *
     * @param user 用户信息
     * @return 用户响应
     */
    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatar(),
                Boolean.TRUE.equals(user.getSuperAdmin()),
                NumberUtils.toIntOrZero(user.getExperience()),
                DateTimeUtils.toOffsetDateTime(user.getCreatedAt()),
                DateTimeUtils.toOffsetDateTime(user.getUpdatedAt())
        );
    }
}
