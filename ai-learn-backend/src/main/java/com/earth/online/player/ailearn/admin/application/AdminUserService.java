package com.earth.online.player.ailearn.admin.application;

import com.earth.online.player.ailearn.admin.interfaces.AdminUserRequest;
import com.earth.online.player.ailearn.admin.interfaces.AdminUserResponse;
import com.earth.online.player.ailearn.admin.interfaces.UserLimitRequest;
import com.earth.online.player.ailearn.admin.interfaces.UserLimitResponse;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;
import com.earth.online.player.ailearn.system.infrastructure.SystemSettingMapper;
import com.earth.online.player.ailearn.user.domain.User;
import com.earth.online.player.ailearn.user.infrastructure.UserMapper;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 管理员用户管理应用服务。
 */
@Service
public class AdminUserService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_EXPERIENCE = 0;
    private static final int DEFAULT_MAX_USERS = 10000;
    private static final int MIN_MAX_USERS = 1;
    private static final int MAX_MAX_USERS = 1_000_000;
    private static final String MAX_USERS_SETTING_KEY = "MAX_USERS";
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,32}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SystemSettingMapper systemSettingMapper;

    /**
     * 创建管理员用户管理服务。
     *
     * @param userMapper 用户仓储
     * @param passwordEncoder 密码编码器
     * @param systemSettingMapper 系统设置仓储
     */
    public AdminUserService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            SystemSettingMapper systemSettingMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.systemSettingMapper = systemSettingMapper;
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
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        String safeKeyword = trimToNull(keyword);
        int offset = (safePageNo - 1) * safePageSize;

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
        AuthenticatedUser operator = requireSuperAdmin();
        User existing = findExisting(id);
        User user = buildUser(request, existing, false);
        if (operator.userId().equals(id) && Boolean.FALSE.equals(user.getSuperAdmin())) {
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
        AuthenticatedUser operator = requireSuperAdmin();
        if (operator.userId().equals(id)) {
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
        systemSettingMapper.upsertValue(MAX_USERS_SETTING_KEY, String.valueOf(maxUsers));
        return new UserLimitResponse(maxUsers, userMapper.countActiveUsers());
    }

    /**
     * 读取系统最大用户数。
     *
     * @return 最大用户数
     */
    public int resolveMaxUsers() {
        String value = systemSettingMapper.findValue(MAX_USERS_SETTING_KEY);
        if (!StringUtils.hasText(value)) {
            return DEFAULT_MAX_USERS;
        }
        try {
            return normalizeMaxUsers(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return DEFAULT_MAX_USERS;
        }
    }

    /**
     * 校验当前用户必须是超级管理员。
     *
     * @return 当前认证用户
     */
    private AuthenticatedUser requireSuperAdmin() {
        AuthenticatedUser authenticatedUser = AuthContext.getUser();
        if (authenticatedUser == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录后即可使用该功能");
        }
        User user = userMapper.findById(authenticatedUser.userId());
        if (user == null || !Boolean.TRUE.equals(user.getSuperAdmin())) {
            throw new BusinessException(ResponseCode.AUTH_FORBIDDEN.code(), "仅超级管理员可维护用户");
        }
        return authenticatedUser;
    }

    /**
     * 查询未删除用户。
     *
     * @param id 用户ID
     * @return 用户信息
     */
    private User findExisting(Long id) {
        if (id == null || id < 1) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "用户ID不合法");
        }
        User user = userMapper.findById(id);
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
        String username = normalizeUsername(request.username());
        String nickname = normalizeNickname(request.nickname());
        String email = normalizeEmail(request.email());
        validateUniqueness(username, nickname, email, existing == null ? null : existing.getId());

        // 新增必须提供密码；编辑时密码为空表示保持原密码。
        User user = new User();
        user.setId(existing == null ? null : existing.getId());
        user.setUsername(username);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setAvatar(trimToNull(request.avatar()));
        user.setPasswordHash(resolvePasswordHash(request.password(), createMode));
        user.setExperience(existing == null ? DEFAULT_EXPERIENCE : existing.getExperience());
        user.setLevelCode(existing == null ? GrowthLevel.resolveByExperience(DEFAULT_EXPERIENCE).code() : existing.getLevelCode());
        user.setRankCode(existing == null ? GrowthRank.resolveByExperience(DEFAULT_EXPERIENCE).code() : existing.getRankCode());
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
        if (password.length() < 8 || password.length() > 64) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "密码长度需为8到64位");
        }
        return passwordEncoder.encode(password);
    }

    /**
     * 规整用户名。
     *
     * @param username 原始用户名
     * @return 安全用户名
     */
    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username) || !USERNAME_PATTERN.matcher(username.trim()).matches()) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "用户名仅支持3到32位字母、数字和下划线");
        }
        return username.trim();
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
     * 规整邮箱。
     *
     * @param email 原始邮箱
     * @return 安全邮箱
     */
    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email) || email.length() > 128 || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "邮箱格式不正确");
        }
        return email.trim();
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
     * 规整页码。
     *
     * @param pageNo 原始页码
     * @return 安全页码
     */
    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < DEFAULT_PAGE_NO ? DEFAULT_PAGE_NO : pageNo;
    }

    /**
     * 规整每页数量。
     *
     * @param pageSize 原始每页数量
     * @return 安全每页数量
     */
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 规整可选文本。
     *
     * @param value 原始文本
     * @return 规整文本
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
                user.getExperience() == null ? 0 : user.getExperience(),
                toOffsetDateTime(user.getCreatedAt()),
                toOffsetDateTime(user.getUpdatedAt())
        );
    }

    /**
     * 转换本地时间。
     *
     * @param value 本地时间
     * @return 带偏移时间
     */
    private OffsetDateTime toOffsetDateTime(java.time.LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
