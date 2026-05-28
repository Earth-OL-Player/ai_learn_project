package com.earth.online.player.ailearn.common.logging.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.logging.ManagedLogService;
import com.earth.online.player.ailearn.common.logging.infrastructure.AiServiceLogLevelClient;
import com.earth.online.player.ailearn.common.logging.interfaces.LogLevelRequest;
import com.earth.online.player.ailearn.common.logging.interfaces.LogLevelResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthSupport;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.user.domain.User;
import com.earth.online.player.ailearn.user.infrastructure.UserMapper;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 管理员日志级别应用服务。
 */
@Service
public class LogLevelAdminService {

    private static final String PROJECT_LOGGER_NAME = "com.earth.online.player.ailearn";
    private static final String SUCCESS_MESSAGE = "日志级别已生效";
    private static final String BACKEND_READY_MESSAGE = "Java 后端日志级别可动态调整";
    private static final String AI_UNAVAILABLE_MESSAGE = "AI 服务不可用，请确认服务已启动且内部 Token 配置一致";
    private static final List<LogLevel> SUPPORTED_LEVELS = List.of(
            LogLevel.TRACE,
            LogLevel.DEBUG,
            LogLevel.INFO,
            LogLevel.WARN,
            LogLevel.ERROR
    );

    private final LoggingSystem loggingSystem;
    private final AiServiceLogLevelClient aiServiceLogLevelClient;
    private final UserMapper userMapper;

    /**
     * 创建日志级别应用服务。
     *
     * @param aiServiceLogLevelClient AI 服务日志级别客户端
     * @param userMapper 用户仓储
     */
    public LogLevelAdminService(
            AiServiceLogLevelClient aiServiceLogLevelClient,
            UserMapper userMapper) {
        this.loggingSystem = LoggingSystem.get(LogLevelAdminService.class.getClassLoader());
        this.aiServiceLogLevelClient = aiServiceLogLevelClient;
        this.userMapper = userMapper;
    }

    /**
     * 查询全部可管理服务的日志级别。
     *
     * @return 日志级别列表
     */
    public List<LogLevelResponse> findAll() {
        requireSuperAdmin();
        return List.of(findBackendLevel(), findAiServiceLevel());
    }

    /**
     * 更新指定服务的日志级别。
     *
     * @param serviceCode 服务编码
     * @param request 日志级别请求
     * @return 更新后日志级别
     */
    public LogLevelResponse update(String serviceCode, LogLevelRequest request) {
        requireSuperAdmin();
        ManagedLogService service = ManagedLogService.resolve(serviceCode);
        LogLevel level = resolveLevel(request == null ? null : request.level());
        if (service == ManagedLogService.BACKEND) {
            return updateBackendLevel(level);
        }
        return aiServiceLogLevelClient.update(level.name());
    }

    /**
     * 查询 Java 后端当前日志级别。
     *
     * @return Java 后端日志级别
     */
    private LogLevelResponse findBackendLevel() {
        return new LogLevelResponse(
                ManagedLogService.BACKEND.code(),
                ManagedLogService.BACKEND.label(),
                resolveBackendLevel().name(),
                true,
                BACKEND_READY_MESSAGE,
                now()
        );
    }

    /**
     * 查询 AI 服务当前日志级别。
     *
     * @return AI 服务日志级别
     */
    private LogLevelResponse findAiServiceLevel() {
        return aiServiceLogLevelClient.find()
                .orElseGet(() -> new LogLevelResponse(
                        ManagedLogService.AI_SERVICE.code(),
                        ManagedLogService.AI_SERVICE.label(),
                        "",
                        false,
                        AI_UNAVAILABLE_MESSAGE,
                        now()
                ));
    }

    /**
     * 更新 Java 后端日志级别。
     *
     * @param level 日志级别
     * @return 更新后日志级别
     */
    private LogLevelResponse updateBackendLevel(LogLevel level) {
        loggingSystem.setLogLevel(LoggingSystem.ROOT_LOGGER_NAME, level);
        loggingSystem.setLogLevel(PROJECT_LOGGER_NAME, level);
        return new LogLevelResponse(
                ManagedLogService.BACKEND.code(),
                ManagedLogService.BACKEND.label(),
                level.name(),
                true,
                SUCCESS_MESSAGE,
                now()
        );
    }

    /**
     * 解析 Java 后端有效日志级别。
     *
     * @return 有效日志级别
     */
    private LogLevel resolveBackendLevel() {
        LoggerConfiguration configuration = loggingSystem.getLoggerConfiguration(PROJECT_LOGGER_NAME);
        if (configuration == null) {
            configuration = loggingSystem.getLoggerConfiguration(LoggingSystem.ROOT_LOGGER_NAME);
        }
        LogLevel level = configuration == null ? null : configuration.getEffectiveLevel();
        return level == null ? LogLevel.INFO : level;
    }

    /**
     * 校验并解析请求日志级别。
     *
     * @param levelName 日志级别文本
     * @return 日志级别
     */
    private LogLevel resolveLevel(String levelName) {
        if (!StringUtils.hasText(levelName)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "日志级别不能为空");
        }
        try {
            LogLevel level = LogLevel.valueOf(levelName.trim().toUpperCase(Locale.ROOT));
            if (SUPPORTED_LEVELS.contains(level)) {
                return level;
            }
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "日志级别不支持");
        }
        throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "日志级别不支持");
    }

    /**
     * 校验当前用户必须是超级管理员。
     */
    private void requireSuperAdmin() {
        AuthenticatedUser authenticatedUser = AuthSupport.requireCurrentUser();
        User user = userMapper.findById(authenticatedUser.userId());
        if (user == null || !Boolean.TRUE.equals(user.getSuperAdmin())) {
            throw new BusinessException(ResponseCode.AUTH_FORBIDDEN.code(), "仅超级管理员可管理日志级别");
        }
    }

    /**
     * 获取当前偏移时间。
     *
     * @return 当前时间
     */
    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneId.systemDefault());
    }
}
