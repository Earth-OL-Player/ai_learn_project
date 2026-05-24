package com.earth.online.player.ailearn.model.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthSupport;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.model.domain.AiModelRequestConfig;
import com.earth.online.player.ailearn.model.domain.ModelAuthorizationProperties;
import com.earth.online.player.ailearn.model.domain.ModelEntitlementKind;
import com.earth.online.player.ailearn.model.domain.ModelEntitlementStatus;
import com.earth.online.player.ailearn.model.domain.ModelLevel;
import com.earth.online.player.ailearn.model.domain.RedemptionCodeStatus;
import com.earth.online.player.ailearn.model.domain.RedemptionCodeType;
import com.earth.online.player.ailearn.model.infrastructure.ModelConfigRecord;
import com.earth.online.player.ailearn.model.infrastructure.ModelEntitlementMapper;
import com.earth.online.player.ailearn.model.infrastructure.RedemptionCodeRecord;
import com.earth.online.player.ailearn.model.infrastructure.UserModelEntitlementRecord;
import com.earth.online.player.ailearn.model.interfaces.AdminModelConfigRequest;
import com.earth.online.player.ailearn.model.interfaces.AdminModelConfigResponse;
import com.earth.online.player.ailearn.model.interfaces.AdminRedemptionCodeGenerateRequest;
import com.earth.online.player.ailearn.model.interfaces.AdminRedemptionCodeResponse;
import com.earth.online.player.ailearn.model.interfaces.AdminRedemptionCodeUpdateRequest;
import com.earth.online.player.ailearn.model.interfaces.ModelEntitlementStatusResponse;
import com.earth.online.player.ailearn.model.interfaces.RedeemModelCodeRequest;
import com.earth.online.player.ailearn.model.interfaces.RedeemModelCodeResponse;
import com.earth.online.player.ailearn.user.domain.User;
import com.earth.online.player.ailearn.user.infrastructure.UserMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 模型权益、兑换码和模型配置应用服务。
 */
@Service
public class ModelEntitlementService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_GENERATE_QUANTITY = 500;
    private static final int MONTHLY_DAYS = 30;
    private static final int RANDOM_CODE_LENGTH = 12;
    private static final String CODE_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String CSV_HEADER = "code,code_type,code_type_text,status,status_text,used_by_username,used_at,created_at\n";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ModelEntitlementMapper modelEntitlementMapper;
    private final UserMapper userMapper;
    private final ModelAuthorizationProperties authorizationProperties;

    /**
     * 创建模型权益应用服务。
     *
     * @param modelEntitlementMapper 模型权益仓储
     * @param userMapper 用户仓储
     * @param authorizationProperties 授权入口配置
     */
    public ModelEntitlementService(
            ModelEntitlementMapper modelEntitlementMapper,
            UserMapper userMapper,
            ModelAuthorizationProperties authorizationProperties) {
        this.modelEntitlementMapper = modelEntitlementMapper;
        this.userMapper = userMapper;
        this.authorizationProperties = authorizationProperties;
    }

    /**
     * 查询当前访问者模型权益展示信息。
     *
     * @return 模型权益展示
     */
    @Transactional
    public ModelEntitlementStatusResponse getCurrentStatus() {
        AuthenticatedUser currentUser = AuthContext.getUser();
        if (currentUser == null) {
            return toStatusResponse(resolveDefaultEntitlement());
        }
        return toStatusResponse(resolveForUser(currentUser.userId(), true));
    }

    /**
     * 兑换模型权益兑换码。
     *
     * @param request 兑换请求
     * @return 兑换结果
     */
    @Transactional(rollbackFor = Exception.class)
    public RedeemModelCodeResponse redeem(RedeemModelCodeRequest request) {
        Long userId = AuthSupport.requireCurrentUserId();
        String code = normalizeRedeemCode(request == null ? null : request.code());
        RedemptionCodeRecord redemptionCode = modelEntitlementMapper.findRedemptionByCodeForUpdate(code);
        if (redemptionCode == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "兑换码不存在");
        }
        if (!RedemptionCodeStatus.UNUSED.name().equals(redemptionCode.getStatus())) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "兑换码已被使用");
        }

        // 兑换前先结算已有权益，保证保护规则基于当前真实权益判断。
        LocalDateTime now = LocalDateTime.now();
        List<UserModelEntitlementRecord> entitlements = modelEntitlementMapper.findUserEntitlementsForUpdate(userId);
        settleAndPersist(entitlements, now);
        RedemptionCodeType codeType = resolveCodeType(redemptionCode.getCodeType());
        validateRedeemAllowed(entitlements, codeType);
        String successMessage = applyRedeemCode(userId, entitlements, codeType, now);
        modelEntitlementMapper.markRedemptionUsed(redemptionCode.getId(), userId, now);
        ResolvedModelEntitlement resolved = resolveFromSettledRecords(entitlements, now, true);
        return new RedeemModelCodeResponse(successMessage, toStatusResponse(resolved));
    }

    /**
     * 解析 AI 调用需要使用的模型配置。
     *
     * @param userId 用户ID
     * @return 当前用户模型权益
     */
    @Transactional
    public ResolvedModelEntitlement resolveForAiCall(Long userId) {
        if (userId == null) {
            return resolveDefaultEntitlement();
        }
        return resolveForUser(userId, true);
    }

    /**
     * 查询管理员模型配置。
     *
     * @return 模型配置列表
     */
    public List<AdminModelConfigResponse> findAdminModelConfigs() {
        requireSuperAdmin("仅超级管理员可维护模型配置");
        List<ModelConfigRecord> records = modelEntitlementMapper.findAllModelConfigs();

        // 返回固定三档配置，缺失记录时使用系统默认模型名兜底。
        return List.of(ModelLevel.BASIC, ModelLevel.PRO, ModelLevel.SUPER).stream()
                .map(level -> toAdminModelConfigResponse(level, findConfigRecord(records, level)))
                .toList();
    }

    /**
     * 保存管理员模型配置。
     *
     * @param levelCode 模型等级编码
     * @param request 保存请求
     * @return 保存后配置
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminModelConfigResponse saveAdminModelConfig(String levelCode, AdminModelConfigRequest request) {
        requireSuperAdmin("仅超级管理员可维护模型配置");
        ModelLevel level = resolveLevel(levelCode);
        ModelConfigRecord record = new ModelConfigRecord();
        record.setModelLevel(level.name());
        record.setModelName(normalizeModelName(request == null ? null : request.modelName(), level));
        record.setBaseUrl(trimToNull(request == null ? null : request.baseUrl()));
        record.setApiKey(trimToNull(request == null ? null : request.apiKey()));
        modelEntitlementMapper.upsertModelConfig(record);
        return toAdminModelConfigResponse(level, modelEntitlementMapper.findModelConfig(level.name()));
    }

    /**
     * 分页查询兑换码。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param keyword 关键词
     * @param codeType 兑换码类型
     * @param status 状态
     * @return 兑换码分页
     */
    public PageResponse<AdminRedemptionCodeResponse> findRedemptionPage(
            Integer pageNo,
            Integer pageSize,
            String keyword,
            String codeType,
            String status) {
        requireSuperAdmin("仅超级管理员可维护兑换码");
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePageNo - 1) * safePageSize;
        String safeKeyword = trimToNull(keyword);
        String safeCodeType = normalizeOptionalCodeType(codeType);
        String safeStatus = normalizeOptionalStatus(status);

        // 管理列表包含未使用和已使用兑换码，已删除记录不展示。
        List<AdminRedemptionCodeResponse> records = modelEntitlementMapper
                .findRedemptionPage(safeKeyword, safeCodeType, safeStatus, offset, safePageSize)
                .stream()
                .map(this::toAdminRedemptionCodeResponse)
                .toList();
        long total = modelEntitlementMapper.countRedemptionPage(safeKeyword, safeCodeType, safeStatus);
        return new PageResponse<>(records, safePageNo, safePageSize, total);
    }

    /**
     * 批量生成兑换码。
     *
     * @param request 生成请求
     * @return 新生成兑换码
     */
    @Transactional(rollbackFor = Exception.class)
    public List<AdminRedemptionCodeResponse> generateRedemptionCodes(AdminRedemptionCodeGenerateRequest request) {
        requireSuperAdmin("仅超级管理员可维护兑换码");
        RedemptionCodeType codeType = resolveCodeType(request == null ? null : request.codeType());
        int quantity = normalizeGenerateQuantity(request == null ? null : request.quantity());
        List<AdminRedemptionCodeResponse> responses = new ArrayList<>();

        // 批量生成时逐个检查唯一性，避免极小概率随机碰撞。
        for (int index = 0; index < quantity; index++) {
            String code = generateUniqueCode(codeType);
            modelEntitlementMapper.insertRedemptionCode(code, codeType.name());
            RedemptionCodeRecord record = modelEntitlementMapper.findRedemptionByCodeForUpdate(code);
            responses.add(toAdminRedemptionCodeResponse(record));
        }
        return responses;
    }

    /**
     * 编辑未使用兑换码类型。
     *
     * @param id 兑换码ID
     * @param request 编辑请求
     * @return 编辑后兑换码
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminRedemptionCodeResponse updateRedemptionCode(Long id, AdminRedemptionCodeUpdateRequest request) {
        requireSuperAdmin("仅超级管理员可维护兑换码");
        RedemptionCodeRecord record = findRedemptionForAdmin(id);
        if (!RedemptionCodeStatus.UNUSED.name().equals(record.getStatus())) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "已使用兑换码不能修改类型");
        }
        RedemptionCodeType codeType = resolveCodeType(request == null ? null : request.codeType());
        modelEntitlementMapper.updateUnusedRedemptionType(record.getId(), codeType.name());
        return toAdminRedemptionCodeResponse(modelEntitlementMapper.findRedemptionByIdForUpdate(record.getId()));
    }

    /**
     * 删除未使用兑换码。
     *
     * @param id 兑换码ID
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRedemptionCode(Long id) {
        requireSuperAdmin("仅超级管理员可维护兑换码");
        RedemptionCodeRecord record = findRedemptionForAdmin(id);
        if (!RedemptionCodeStatus.UNUSED.name().equals(record.getStatus())) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "已使用兑换码不能删除");
        }
        int affected = modelEntitlementMapper.deleteUnusedRedemption(record.getId());
        if (affected == 0) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "兑换码状态已变化，请刷新后重试");
        }
        return true;
    }

    /**
     * 导出兑换码 CSV。
     *
     * @param keyword 关键词
     * @param codeType 兑换码类型
     * @param status 状态
     * @return CSV 字节
     */
    public byte[] exportRedemptionCodes(String keyword, String codeType, String status) {
        requireSuperAdmin("仅超级管理员可维护兑换码");
        String safeKeyword = trimToNull(keyword);
        String safeCodeType = normalizeOptionalCodeType(codeType);
        String safeStatus = normalizeOptionalStatus(status);
        StringBuilder builder = new StringBuilder("\uFEFF").append(CSV_HEADER);

        // 导出只包含当前筛选结果，已删除兑换码不进入文件。
        for (RedemptionCodeRecord record : modelEntitlementMapper.findRedemptionsForExport(safeKeyword, safeCodeType, safeStatus)) {
            appendCsvRow(builder, record);
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 查询用户已结算权益。
     *
     * @param userId 用户ID
     * @param persist 是否持久化结算结果
     * @return 已解析权益
     */
    private ResolvedModelEntitlement resolveForUser(Long userId, boolean persist) {
        LocalDateTime now = LocalDateTime.now();
        List<UserModelEntitlementRecord> entitlements = persist
                ? modelEntitlementMapper.findUserEntitlementsForUpdate(userId)
                : modelEntitlementMapper.findUserEntitlements(userId);
        return resolveFromSettledRecords(entitlements, now, persist);
    }

    /**
     * 基于已有权益记录完成结算并解析当前权益。
     *
     * @param entitlements 权益记录
     * @param now 当前时间
     * @param persist 是否持久化
     * @return 已解析权益
     */
    private ResolvedModelEntitlement resolveFromSettledRecords(
            List<UserModelEntitlementRecord> entitlements,
            LocalDateTime now,
            boolean persist) {
        settleEntitlements(entitlements, now);
        if (persist) {
            persistEntitlements(entitlements);
        }
        ModelLevel level = resolveEffectiveLevel(entitlements);
        boolean permanent = isPermanentEffective(entitlements, level);
        int remainingDays = permanent ? 0 : remainingDays(entitlements, level);
        int frozenProDays = frozenProRemainingDays(entitlements);
        return new ResolvedModelEntitlement(level, resolveModelName(level), remainingDays, permanent, frozenProDays, resolveRequestConfig(level));
    }

    /**
     * 构建默认初级权益。
     *
     * @return 默认权益
     */
    private ResolvedModelEntitlement resolveDefaultEntitlement() {
        ModelLevel level = ModelLevel.BASIC;
        return new ResolvedModelEntitlement(level, resolveModelName(level), 0, true, 0, resolveRequestConfig(level));
    }

    /**
     * 结算并持久化权益。
     *
     * @param entitlements 权益记录
     * @param now 当前时间
     */
    private void settleAndPersist(List<UserModelEntitlementRecord> entitlements, LocalDateTime now) {
        settleEntitlements(entitlements, now);
        persistEntitlements(entitlements);
    }

    /**
     * 结算权益剩余天数和冻结状态。
     *
     * @param entitlements 权益记录
     * @param now 当前时间
     */
    private void settleEntitlements(List<UserModelEntitlementRecord> entitlements, LocalDateTime now) {
        entitlements.forEach(record -> settleMonthlyConsumption(record, now));
        coverMonthlyByPermanent(entitlements);
        reconcileSuperMonthlyFreeze(entitlements, now);
    }

    /**
     * 结算单条月度权益消耗。
     *
     * @param record 权益记录
     * @param now 当前时间
     */
    private void settleMonthlyConsumption(UserModelEntitlementRecord record, LocalDateTime now) {
        if (!ModelEntitlementKind.MONTHLY.name().equals(record.getEntitlementKind())) {
            return;
        }
        if (!ModelEntitlementStatus.ACTIVE.name().equals(record.getStatus())) {
            return;
        }
        int remainingDays = safeRemainingDays(record);
        if (remainingDays <= 0) {
            expireMonthly(record);
            return;
        }
        LocalDateTime lastConsumedAt = record.getLastConsumedAt() == null ? now : record.getLastConsumedAt();
        long elapsedDays = ChronoUnit.DAYS.between(lastConsumedAt, now);
        if (elapsedDays <= 0) {
            return;
        }

        // 按完整 24 小时扣减，剩余不足一天时仍展示为 1 天，直到下一次结算到 0。
        int nextRemainingDays = remainingDays - Math.toIntExact(Math.min(elapsedDays, remainingDays));
        if (nextRemainingDays <= 0) {
            expireMonthly(record);
            return;
        }
        record.setRemainingDays(nextRemainingDays);
        record.setLastConsumedAt(lastConsumedAt.plusDays(elapsedDays));
    }

    /**
     * 永久权益覆盖同等级或更低等级月度权益。
     *
     * @param entitlements 权益记录
     */
    private void coverMonthlyByPermanent(List<UserModelEntitlementRecord> entitlements) {
        boolean superPermanent = hasActivePermanent(entitlements, ModelLevel.SUPER);
        boolean proPermanent = hasActivePermanent(entitlements, ModelLevel.PRO);
        if (superPermanent) {
            entitlements.stream()
                    .filter(this::isRecoverableMonthly)
                    .forEach(record -> markStatus(record, ModelEntitlementStatus.COVERED, null));
            entitlements.stream()
                    .filter(record -> isPermanent(record, ModelLevel.PRO))
                    .forEach(record -> markStatus(record, ModelEntitlementStatus.UPGRADED, null));
            return;
        }
        if (proPermanent) {
            entitlements.stream()
                    .filter(record -> isMonthly(record, ModelLevel.PRO))
                    .filter(record -> !ModelEntitlementStatus.COVERED.name().equals(record.getStatus()))
                    .forEach(record -> markStatus(record, ModelEntitlementStatus.COVERED, null));
        }
    }

    /**
     * 处理超级月度生效时高级月度冻结和恢复。
     *
     * @param entitlements 权益记录
     * @param now 当前时间
     */
    private void reconcileSuperMonthlyFreeze(List<UserModelEntitlementRecord> entitlements, LocalDateTime now) {
        boolean superMonthlyActive = entitlements.stream().anyMatch(record -> isActiveMonthly(record, ModelLevel.SUPER));
        entitlements.stream()
                .filter(record -> isMonthly(record, ModelLevel.PRO))
                .filter(record -> !ModelEntitlementStatus.COVERED.name().equals(record.getStatus()))
                .filter(record -> !ModelEntitlementStatus.EXPIRED.name().equals(record.getStatus()))
                .forEach(record -> reconcileProMonthlyStatus(record, superMonthlyActive, now));
    }

    /**
     * 处理高级月度冻结或恢复。
     *
     * @param record 权益记录
     * @param superMonthlyActive 超级月度是否生效
     * @param now 当前时间
     */
    private void reconcileProMonthlyStatus(UserModelEntitlementRecord record, boolean superMonthlyActive, LocalDateTime now) {
        if (safeRemainingDays(record) <= 0) {
            expireMonthly(record);
            return;
        }
        if (superMonthlyActive) {
            markStatus(record, ModelEntitlementStatus.FROZEN, null);
            return;
        }
        if (ModelEntitlementStatus.FROZEN.name().equals(record.getStatus())) {
            markStatus(record, ModelEntitlementStatus.ACTIVE, now);
        }
    }

    /**
     * 持久化权益结算结果。
     *
     * @param entitlements 权益记录
     */
    private void persistEntitlements(List<UserModelEntitlementRecord> entitlements) {
        entitlements.stream()
                .filter(record -> record.getId() != null)
                .forEach(modelEntitlementMapper::updateUserEntitlement);
    }

    /**
     * 校验兑换是否允许。
     *
     * @param entitlements 已结算权益
     * @param codeType 兑换码类型
     */
    private void validateRedeemAllowed(List<UserModelEntitlementRecord> entitlements, RedemptionCodeType codeType) {
        if (hasActivePermanent(entitlements, ModelLevel.SUPER)) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "当前权益已高于兑换码，无需兑换");
        }
        if (hasActivePermanent(entitlements, ModelLevel.PRO)
                && (codeType == RedemptionCodeType.PRO_MONTHLY || codeType == RedemptionCodeType.PRO_PERMANENT)) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "当前权益已高于兑换码，无需兑换");
        }
        if (codeType == RedemptionCodeType.PRO_PERMANENT_TO_SUPER && !hasActivePermanent(entitlements, ModelLevel.PRO)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "当前不是高级模型永久用户，无法使用该兑换码");
        }
    }

    /**
     * 应用兑换码权益。
     *
     * @param userId 用户ID
     * @param entitlements 权益记录
     * @param codeType 兑换码类型
     * @param now 当前时间
     * @return 成功提示
     */
    private String applyRedeemCode(
            Long userId,
            List<UserModelEntitlementRecord> entitlements,
            RedemptionCodeType codeType,
            LocalDateTime now) {
        if (codeType.entitlementKind() == ModelEntitlementKind.MONTHLY) {
            applyMonthlyEntitlement(userId, entitlements, codeType.targetLevel(), now);
            settleAndPersist(entitlements, now);
            return "兑换成功，已获得" + codeType.targetLevel().label() + " 30 天权益";
        }
        applyPermanentEntitlement(userId, entitlements, codeType, now);
        settleAndPersist(entitlements, now);
        return codeType == RedemptionCodeType.PRO_PERMANENT_TO_SUPER
                ? "兑换成功，已升级为超级模型永久权益"
                : "兑换成功，已获得" + codeType.targetLevel().label() + "永久权益";
    }

    /**
     * 应用月度权益。
     *
     * @param userId 用户ID
     * @param entitlements 权益记录
     * @param level 模型等级
     * @param now 当前时间
     */
    private void applyMonthlyEntitlement(
            Long userId,
            List<UserModelEntitlementRecord> entitlements,
            ModelLevel level,
            LocalDateTime now) {
        UserModelEntitlementRecord record = findEntitlement(entitlements, level, ModelEntitlementKind.MONTHLY);
        if (record == null) {
            record = newEntitlement(userId, level, ModelEntitlementKind.MONTHLY, now);
            entitlements.add(record);
        }

        // 同等级月度直接叠加 30 天；被过期记录复用时从当前时间重新开始扣减。
        int previousDays = ModelEntitlementStatus.EXPIRED.name().equals(record.getStatus()) ? 0 : safeRemainingDays(record);
        record.setRemainingDays(previousDays + MONTHLY_DAYS);
        record.setStartedAt(record.getStartedAt() == null ? now : record.getStartedAt());
        record.setStatus(shouldFreezeMonthly(level, entitlements) ? ModelEntitlementStatus.FROZEN.name() : ModelEntitlementStatus.ACTIVE.name());
        record.setLastConsumedAt(ModelEntitlementStatus.ACTIVE.name().equals(record.getStatus()) ? resolveLastConsumedAt(record, now) : null);
        persistNewEntitlement(record);
    }

    /**
     * 应用永久权益。
     *
     * @param userId 用户ID
     * @param entitlements 权益记录
     * @param codeType 兑换码类型
     * @param now 当前时间
     */
    private void applyPermanentEntitlement(
            Long userId,
            List<UserModelEntitlementRecord> entitlements,
            RedemptionCodeType codeType,
            LocalDateTime now) {
        UserModelEntitlementRecord record = findEntitlement(entitlements, codeType.targetLevel(), ModelEntitlementKind.PERMANENT);
        if (record == null) {
            record = newEntitlement(userId, codeType.targetLevel(), ModelEntitlementKind.PERMANENT, now);
            entitlements.add(record);
        }
        record.setStatus(ModelEntitlementStatus.ACTIVE.name());
        record.setRemainingDays(0);
        record.setLastConsumedAt(null);
        record.setStartedAt(record.getStartedAt() == null ? now : record.getStartedAt());
        persistNewEntitlement(record);

        // 高级永久升超成功后，高级永久只保留历史标记，不再作为有效永久权益。
        if (codeType.targetLevel() == ModelLevel.SUPER) {
            entitlements.stream()
                    .filter(item -> isPermanent(item, ModelLevel.PRO))
                    .forEach(item -> markStatus(item, ModelEntitlementStatus.UPGRADED, null));
        }
    }

    /**
     * 新建权益记录。
     *
     * @param userId 用户ID
     * @param level 模型等级
     * @param kind 权益类型
     * @param now 当前时间
     * @return 权益记录
     */
    private UserModelEntitlementRecord newEntitlement(
            Long userId,
            ModelLevel level,
            ModelEntitlementKind kind,
            LocalDateTime now) {
        UserModelEntitlementRecord record = new UserModelEntitlementRecord();
        record.setUserId(userId);
        record.setModelLevel(level.name());
        record.setEntitlementKind(kind.name());
        record.setStatus(ModelEntitlementStatus.ACTIVE.name());
        record.setRemainingDays(0);
        record.setStartedAt(now);
        return record;
    }

    /**
     * 持久化新权益记录。
     *
     * @param record 权益记录
     */
    private void persistNewEntitlement(UserModelEntitlementRecord record) {
        if (record.getId() == null) {
            modelEntitlementMapper.insertUserEntitlement(record);
        }
    }

    /**
     * 判断月度权益是否需要冻结。
     *
     * @param level 模型等级
     * @param entitlements 权益记录
     * @return 是否冻结
     */
    private boolean shouldFreezeMonthly(ModelLevel level, List<UserModelEntitlementRecord> entitlements) {
        return level == ModelLevel.PRO && entitlements.stream().anyMatch(record -> isActiveMonthly(record, ModelLevel.SUPER));
    }

    /**
     * 获取月度扣减起点。
     *
     * @param record 权益记录
     * @param now 当前时间
     * @return 扣减起点
     */
    private LocalDateTime resolveLastConsumedAt(UserModelEntitlementRecord record, LocalDateTime now) {
        return record.getLastConsumedAt() == null || ModelEntitlementStatus.EXPIRED.name().equals(record.getStatus())
                ? now
                : record.getLastConsumedAt();
    }

    /**
     * 解析当前生效模型等级。
     *
     * @param entitlements 权益记录
     * @return 模型等级
     */
    private ModelLevel resolveEffectiveLevel(List<UserModelEntitlementRecord> entitlements) {
        if (hasActivePermanent(entitlements, ModelLevel.SUPER) || hasActiveMonthly(entitlements, ModelLevel.SUPER)) {
            return ModelLevel.SUPER;
        }
        if (hasActivePermanent(entitlements, ModelLevel.PRO) || hasActiveMonthly(entitlements, ModelLevel.PRO)) {
            return ModelLevel.PRO;
        }
        return ModelLevel.BASIC;
    }

    /**
     * 判断永久权益是否当前生效。
     *
     * @param entitlements 权益记录
     * @param level 模型等级
     * @return 是否生效
     */
    private boolean isPermanentEffective(List<UserModelEntitlementRecord> entitlements, ModelLevel level) {
        if (level == ModelLevel.BASIC) {
            return true;
        }
        return hasActivePermanent(entitlements, level);
    }

    /**
     * 查询剩余天数。
     *
     * @param entitlements 权益记录
     * @param level 模型等级
     * @return 剩余天数
     */
    private int remainingDays(List<UserModelEntitlementRecord> entitlements, ModelLevel level) {
        return entitlements.stream()
                .filter(record -> isActiveMonthly(record, level))
                .mapToInt(this::safeRemainingDays)
                .findFirst()
                .orElse(0);
    }

    /**
     * 查询冻结高级月度天数。
     *
     * @param entitlements 权益记录
     * @return 冻结天数
     */
    private int frozenProRemainingDays(List<UserModelEntitlementRecord> entitlements) {
        return entitlements.stream()
                .filter(record -> isMonthly(record, ModelLevel.PRO))
                .filter(record -> ModelEntitlementStatus.FROZEN.name().equals(record.getStatus()))
                .mapToInt(this::safeRemainingDays)
                .findFirst()
                .orElse(0);
    }

    /**
     * 转换权益展示响应。
     *
     * @param resolved 已解析权益
     * @return 展示响应
     */
    private ModelEntitlementStatusResponse toStatusResponse(ResolvedModelEntitlement resolved) {
        String authorizationUrl = normalizeAuthorizationUrl(authorizationProperties.getUrl());
        String remainingDaysText = resolved.permanent() ? "永久" : Math.max(1, resolved.remainingDays()) + "天";
        String frozenTip = buildFrozenTip(resolved);
        return new ModelEntitlementStatusResponse(
                resolved.level().name(),
                resolved.level().label(),
                resolved.modelName(),
                resolved.remainingDays(),
                remainingDaysText,
                resolved.permanent(),
                shouldShowAuthorization(resolved),
                authorizationButtonText(resolved),
                authorizationUrl,
                StringUtils.hasText(authorizationUrl),
                frozenTip,
                resolved.frozenProRemainingDays()
        );
    }

    /**
     * 判断是否展示授权按钮。
     *
     * @param resolved 已解析权益
     * @return 是否展示
     */
    private boolean shouldShowAuthorization(ResolvedModelEntitlement resolved) {
        return !(resolved.level() == ModelLevel.SUPER && resolved.permanent());
    }

    /**
     * 生成授权按钮文案。
     *
     * @param resolved 已解析权益
     * @return 按钮文案
     */
    private String authorizationButtonText(ResolvedModelEntitlement resolved) {
        // 授权入口统一承载高级模型授权说明，避免按钮文案随当前权益频繁变化。
        return shouldShowAuthorization(resolved) ? "高级模型授权" : "";
    }

    /**
     * 构造冻结提示。
     *
     * @param resolved 已解析权益
     * @return 冻结提示
     */
    private String buildFrozenTip(ResolvedModelEntitlement resolved) {
        if (resolved.level() == ModelLevel.SUPER && !resolved.permanent() && resolved.frozenProRemainingDays() > 0) {
            return "当前剩余高级模型 " + resolved.frozenProRemainingDays() + " 天已冻结，超级模型天数结束后降级为高级模型";
        }
        return "";
    }

    /**
     * 解析模型展示名称。
     *
     * @param level 模型等级
     * @return 模型名称
     */
    private String resolveModelName(ModelLevel level) {
        ModelConfigRecord record = modelEntitlementMapper.findModelConfig(level.name());
        if (record != null && StringUtils.hasText(record.getModelName())) {
            return record.getModelName().trim();
        }
        return level.defaultModelName();
    }

    /**
     * 解析请求级模型配置。
     *
     * @param level 模型等级
     * @return 请求级模型配置
     */
    private AiModelRequestConfig resolveRequestConfig(ModelLevel level) {
        ModelConfigRecord record = modelEntitlementMapper.findModelConfig(level.name());
        if (record == null) {
            return new AiModelRequestConfig(level.defaultModelName(), "", "");
        }
        return new AiModelRequestConfig(
                StringUtils.hasText(record.getModelName()) ? record.getModelName().trim() : level.defaultModelName(),
                trimToEmpty(record.getBaseUrl()),
                trimToEmpty(record.getApiKey())
        );
    }

    /**
     * 兑换码 CSV 追加一行。
     *
     * @param builder CSV 构造器
     * @param record 兑换码记录
     */
    private void appendCsvRow(StringBuilder builder, RedemptionCodeRecord record) {
        RedemptionCodeType codeType = resolveCodeType(record.getCodeType());
        builder.append(csv(record.getCode())).append(',')
                .append(csv(record.getCodeType())).append(',')
                .append(csv(codeType.label())).append(',')
                .append(csv(record.getStatus())).append(',')
                .append(csv(statusText(record.getStatus()))).append(',')
                .append(csv(record.getUsedByUsername())).append(',')
                .append(csv(toOffsetDateTime(record.getUsedAt()))).append(',')
                .append(csv(toOffsetDateTime(record.getCreatedAt()))).append('\n');
    }

    /**
     * CSV 字段转义。
     *
     * @param value 原始值
     * @return 转义值
     */
    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (!text.contains(",") && !text.contains("\"") && !text.contains("\n")) {
            return text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    /**
     * 转换管理员模型配置响应。
     *
     * @param level 模型等级
     * @param record 配置记录
     * @return 配置响应
     */
    private AdminModelConfigResponse toAdminModelConfigResponse(ModelLevel level, ModelConfigRecord record) {
        return new AdminModelConfigResponse(
                level.name(),
                level.label(),
                record == null || !StringUtils.hasText(record.getModelName()) ? level.defaultModelName() : record.getModelName(),
                record == null ? "" : trimToEmpty(record.getBaseUrl()),
                record == null ? "" : trimToEmpty(record.getApiKey()),
                record == null ? null : toOffsetDateTime(record.getUpdatedAt())
        );
    }

    /**
     * 转换管理员兑换码响应。
     *
     * @param record 兑换码记录
     * @return 兑换码响应
     */
    private AdminRedemptionCodeResponse toAdminRedemptionCodeResponse(RedemptionCodeRecord record) {
        RedemptionCodeType codeType = resolveCodeType(record.getCodeType());
        boolean unused = RedemptionCodeStatus.UNUSED.name().equals(record.getStatus());
        return new AdminRedemptionCodeResponse(
                String.valueOf(record.getId()),
                record.getCode(),
                record.getCodeType(),
                codeType.label(),
                record.getStatus(),
                statusText(record.getStatus()),
                record.getUsedByUserId() == null ? "" : String.valueOf(record.getUsedByUserId()),
                record.getUsedByUsername(),
                toOffsetDateTime(record.getUsedAt()),
                toOffsetDateTime(record.getCreatedAt()),
                unused,
                unused
        );
    }

    /**
     * 查询管理员操作的兑换码。
     *
     * @param id 兑换码ID
     * @return 兑换码记录
     */
    private RedemptionCodeRecord findRedemptionForAdmin(Long id) {
        if (id == null || id < 1) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "兑换码ID不合法");
        }
        RedemptionCodeRecord record = modelEntitlementMapper.findRedemptionByIdForUpdate(id);
        if (record == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "兑换码不存在或已删除");
        }
        return record;
    }

    /**
     * 要求当前用户是超级管理员。
     *
     * @param forbiddenMessage 无权限提示
     */
    private void requireSuperAdmin(String forbiddenMessage) {
        AuthenticatedUser authenticatedUser = AuthContext.getUser();
        if (authenticatedUser == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录后即可使用该功能");
        }
        User user = userMapper.findById(authenticatedUser.userId());
        if (user == null || !Boolean.TRUE.equals(user.getSuperAdmin())) {
            throw new BusinessException(ResponseCode.AUTH_FORBIDDEN.code(), forbiddenMessage);
        }
    }

    /**
     * 生成唯一兑换码。
     *
     * @param codeType 兑换码类型
     * @return 兑换码
     */
    private String generateUniqueCode(RedemptionCodeType codeType) {
        for (int retry = 0; retry < 10; retry++) {
            String code = codeType.prefix() + "-" + randomCodeSuffix();
            if (modelEntitlementMapper.findRedemptionIdByCode(code) == null) {
                return code;
            }
        }
        throw new BusinessException(ResponseCode.SYSTEM_ERROR.code(), "兑换码生成失败，请重试");
    }

    /**
     * 生成兑换码随机后缀。
     *
     * @return 随机后缀
     */
    private String randomCodeSuffix() {
        StringBuilder builder = new StringBuilder(RANDOM_CODE_LENGTH);
        for (int index = 0; index < RANDOM_CODE_LENGTH; index++) {
            builder.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return builder.toString();
    }

    /**
     * 查询指定模型配置。
     *
     * @param records 配置列表
     * @param level 模型等级
     * @return 配置记录
     */
    private ModelConfigRecord findConfigRecord(List<ModelConfigRecord> records, ModelLevel level) {
        return records.stream()
                .filter(record -> level.name().equals(record.getModelLevel()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 查询指定权益。
     *
     * @param entitlements 权益列表
     * @param level 模型等级
     * @param kind 权益类型
     * @return 权益记录
     */
    private UserModelEntitlementRecord findEntitlement(
            List<UserModelEntitlementRecord> entitlements,
            ModelLevel level,
            ModelEntitlementKind kind) {
        return entitlements.stream()
                .filter(record -> level.name().equals(record.getModelLevel()))
                .filter(record -> kind.name().equals(record.getEntitlementKind()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断是否存在生效永久权益。
     *
     * @param entitlements 权益列表
     * @param level 模型等级
     * @return 是否存在
     */
    private boolean hasActivePermanent(List<UserModelEntitlementRecord> entitlements, ModelLevel level) {
        return entitlements.stream().anyMatch(record -> isPermanent(record, level)
                && ModelEntitlementStatus.ACTIVE.name().equals(record.getStatus()));
    }

    /**
     * 判断是否存在生效月度权益。
     *
     * @param entitlements 权益列表
     * @param level 模型等级
     * @return 是否存在
     */
    private boolean hasActiveMonthly(List<UserModelEntitlementRecord> entitlements, ModelLevel level) {
        return entitlements.stream().anyMatch(record -> isActiveMonthly(record, level));
    }

    /**
     * 判断记录是否为生效月度权益。
     *
     * @param record 权益记录
     * @param level 模型等级
     * @return 是否生效
     */
    private boolean isActiveMonthly(UserModelEntitlementRecord record, ModelLevel level) {
        return isMonthly(record, level)
                && ModelEntitlementStatus.ACTIVE.name().equals(record.getStatus())
                && safeRemainingDays(record) > 0;
    }

    /**
     * 判断是否为可恢复月度权益。
     *
     * @param record 权益记录
     * @return 是否可恢复
     */
    private boolean isRecoverableMonthly(UserModelEntitlementRecord record) {
        return ModelEntitlementKind.MONTHLY.name().equals(record.getEntitlementKind())
                && !ModelEntitlementStatus.COVERED.name().equals(record.getStatus())
                && !ModelEntitlementStatus.EXPIRED.name().equals(record.getStatus());
    }

    /**
     * 判断是否为指定月度权益。
     *
     * @param record 权益记录
     * @param level 模型等级
     * @return 是否匹配
     */
    private boolean isMonthly(UserModelEntitlementRecord record, ModelLevel level) {
        return level.name().equals(record.getModelLevel())
                && ModelEntitlementKind.MONTHLY.name().equals(record.getEntitlementKind());
    }

    /**
     * 判断是否为指定永久权益。
     *
     * @param record 权益记录
     * @param level 模型等级
     * @return 是否匹配
     */
    private boolean isPermanent(UserModelEntitlementRecord record, ModelLevel level) {
        return level.name().equals(record.getModelLevel())
                && ModelEntitlementKind.PERMANENT.name().equals(record.getEntitlementKind());
    }

    /**
     * 标记权益状态。
     *
     * @param record 权益记录
     * @param status 新状态
     * @param lastConsumedAt 最近扣减时间
     */
    private void markStatus(UserModelEntitlementRecord record, ModelEntitlementStatus status, LocalDateTime lastConsumedAt) {
        record.setStatus(status.name());
        record.setLastConsumedAt(lastConsumedAt);
    }

    /**
     * 标记月度权益过期。
     *
     * @param record 权益记录
     */
    private void expireMonthly(UserModelEntitlementRecord record) {
        record.setStatus(ModelEntitlementStatus.EXPIRED.name());
        record.setRemainingDays(0);
        record.setLastConsumedAt(null);
    }

    /**
     * 获取安全剩余天数。
     *
     * @param record 权益记录
     * @return 剩余天数
     */
    private int safeRemainingDays(UserModelEntitlementRecord record) {
        return record.getRemainingDays() == null ? 0 : Math.max(0, record.getRemainingDays());
    }

    /**
     * 解析模型等级。
     *
     * @param levelCode 等级编码
     * @return 模型等级
     */
    private ModelLevel resolveLevel(String levelCode) {
        try {
            return ModelLevel.resolve(levelCode);
        } catch (RuntimeException exception) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "模型等级不合法");
        }
    }

    /**
     * 解析兑换码类型。
     *
     * @param codeType 类型编码
     * @return 兑换码类型
     */
    private RedemptionCodeType resolveCodeType(String codeType) {
        if (!StringUtils.hasText(codeType)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "兑换码类型不能为空");
        }
        try {
            return RedemptionCodeType.resolve(codeType);
        } catch (RuntimeException exception) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "兑换码类型不合法");
        }
    }

    /**
     * 规整可选兑换码类型。
     *
     * @param codeType 类型编码
     * @return 安全编码
     */
    private String normalizeOptionalCodeType(String codeType) {
        return StringUtils.hasText(codeType) ? resolveCodeType(codeType).name() : null;
    }

    /**
     * 规整可选兑换码状态。
     *
     * @param status 状态编码
     * @return 安全编码
     */
    private String normalizeOptionalStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return RedemptionCodeStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
        } catch (RuntimeException exception) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "兑换码状态不合法");
        }
    }

    /**
     * 规整模型名称。
     *
     * @param modelName 模型名称
     * @param level 模型等级
     * @return 安全模型名称
     */
    private String normalizeModelName(String modelName, ModelLevel level) {
        if (!StringUtils.hasText(modelName)) {
            return level.defaultModelName();
        }
        String safeModelName = modelName.trim();
        if (safeModelName.length() > 128) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "模型名称不能超过128个字符");
        }
        return safeModelName;
    }

    /**
     * 规整兑换码。
     *
     * @param code 兑换码
     * @return 安全兑换码
     */
    private String normalizeRedeemCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "兑换码不能为空");
        }
        String safeCode = code.trim();
        if (safeCode.length() > 32) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "兑换码不合法");
        }
        return safeCode;
    }

    /**
     * 规整生成数量。
     *
     * @param quantity 原始数量
     * @return 安全数量
     */
    private int normalizeGenerateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > MAX_GENERATE_QUANTITY) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "生成数量必须在1到500之间");
        }
        return quantity;
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
     * 转换状态文案。
     *
     * @param status 状态编码
     * @return 状态文案
     */
    private String statusText(String status) {
        if (RedemptionCodeStatus.USED.name().equals(status)) {
            return "已使用";
        }
        return "未使用";
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
     * 规整授权入口完整网址。
     *
     * @param value 原始授权地址
     * @return 安全授权地址
     */
    private String normalizeAuthorizationUrl(String value) {
        String safeUrl = trimToEmpty(value);
        if (!StringUtils.hasText(safeUrl) || !isHttpWebsiteUrl(safeUrl)) {
            return "";
        }
        return safeUrl;
    }

    /**
     * 判断是否为完整网站地址。
     *
     * @param value 原始地址
     * @return 是否为 http 或 https 绝对网址
     */
    private boolean isHttpWebsiteUrl(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();

            // 授权入口必须是带协议和域名的完整网站地址，避免相对路径误跳转。
            return uri.isAbsolute()
                    && StringUtils.hasText(uri.getHost())
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * 规整文本为空字符串。
     *
     * @param value 原始文本
     * @return 安全文本
     */
    private String trimToEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    /**
     * 转换本地时间。
     *
     * @param value 本地时间
     * @return 带偏移时间
     */
    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
