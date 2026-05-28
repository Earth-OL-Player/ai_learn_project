package com.earth.online.player.ailearn.model.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 模型权益 MyBatis 仓储。
 */
@Mapper
public interface ModelEntitlementMapper {

    /**
     * 查询全部模型配置。
     *
     * @return 模型配置列表
     */
    @Select("""
            SELECT id, model_level, model_name, base_url, api_key, created_at, updated_at
            FROM model_configs
            ORDER BY FIELD(model_level, 'BASIC', 'PRO', 'SUPER')
            """)
    List<ModelConfigRecord> findAllModelConfigs();

    /**
     * 按等级查询模型配置。
     *
     * @param modelLevel 模型等级
     * @return 模型配置
     */
    @Select("""
            SELECT id, model_level, model_name, base_url, api_key, created_at, updated_at
            FROM model_configs
            WHERE model_level = #{modelLevel}
            """)
    ModelConfigRecord findModelConfig(@Param("modelLevel") String modelLevel);

    /**
     * 新增或更新模型配置。
     *
     * @param record 模型配置
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO model_configs(model_level, model_name, base_url, api_key)
            VALUES(#{modelLevel}, #{modelName}, #{baseUrl}, #{apiKey})
            ON DUPLICATE KEY UPDATE model_name = VALUES(model_name),
                                    base_url = VALUES(base_url),
                                    api_key = VALUES(api_key)
            """)
    int upsertModelConfig(ModelConfigRecord record);

    /**
     * 查询用户权益并加锁。
     *
     * @param userId 用户ID
     * @return 权益列表
     */
    @Select("""
            SELECT id, user_id, model_level, entitlement_kind, status, remaining_days,
                   last_consumed_at, started_at, created_at, updated_at
            FROM user_model_entitlements
            WHERE user_id = #{userId}
            FOR UPDATE
            """)
    List<UserModelEntitlementRecord> findUserEntitlementsForUpdate(@Param("userId") Long userId);

    /**
     * 查询用户权益。
     *
     * @param userId 用户ID
     * @return 权益列表
     */
    @Select("""
            SELECT id, user_id, model_level, entitlement_kind, status, remaining_days,
                   last_consumed_at, started_at, created_at, updated_at
            FROM user_model_entitlements
            WHERE user_id = #{userId}
            """)
    List<UserModelEntitlementRecord> findUserEntitlements(@Param("userId") Long userId);

    /**
     * 按用户、等级和类型查询权益。
     *
     * @param userId 用户ID
     * @param modelLevel 模型等级
     * @param entitlementKind 权益类型
     * @return 权益记录
     */
    @Select("""
            SELECT id, user_id, model_level, entitlement_kind, status, remaining_days,
                   last_consumed_at, started_at, created_at, updated_at
            FROM user_model_entitlements
            WHERE user_id = #{userId}
              AND model_level = #{modelLevel}
              AND entitlement_kind = #{entitlementKind}
            """)
    UserModelEntitlementRecord findUserEntitlement(
            @Param("userId") Long userId,
            @Param("modelLevel") String modelLevel,
            @Param("entitlementKind") String entitlementKind
    );

    /**
     * 新增用户权益。
     *
     * @param record 权益记录
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO user_model_entitlements(user_id, model_level, entitlement_kind, status,
                                                remaining_days, last_consumed_at, started_at)
            VALUES(#{userId}, #{modelLevel}, #{entitlementKind}, #{status},
                   #{remainingDays}, #{lastConsumedAt}, #{startedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertUserEntitlement(UserModelEntitlementRecord record);

    /**
     * 更新用户权益。
     *
     * @param record 权益记录
     * @return 影响行数
     */
    @Update("""
            UPDATE user_model_entitlements
            SET status = #{status},
                remaining_days = #{remainingDays},
                last_consumed_at = #{lastConsumedAt},
                started_at = #{startedAt}
            WHERE id = #{id}
            """)
    int updateUserEntitlement(UserModelEntitlementRecord record);

    /**
     * 分页查询兑换码。
     *
     * @param keyword 关键词
     * @param codeType 兑换码类型
     * @param status 兑换码状态
     * @param offset 偏移量
     * @param pageSize 每页数量
     * @return 兑换码列表
     */
    @Select({
            "<script>",
            ModelEntitlementSql.REDEMPTION_CODE_SELECT_COLUMNS,
            ModelEntitlementSql.REDEMPTION_CODE_FILTER_SQL,
            ModelEntitlementSql.REDEMPTION_CODE_ORDER_SQL,
            "LIMIT #{pageSize} OFFSET #{offset}",
            "</script>"
    })
    List<RedemptionCodeRecord> findRedemptionPage(
            @Param("keyword") String keyword,
            @Param("codeType") String codeType,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计兑换码数量。
     *
     * @param keyword 关键词
     * @param codeType 兑换码类型
     * @param status 兑换码状态
     * @return 总数
     */
    @Select({
            "<script>",
            "SELECT COUNT(1)",
            ModelEntitlementSql.REDEMPTION_CODE_FILTER_SQL,
            "</script>"
    })
    long countRedemptionPage(
            @Param("keyword") String keyword,
            @Param("codeType") String codeType,
            @Param("status") String status
    );

    /**
     * 查询导出兑换码。
     *
     * @param keyword 关键词
     * @param codeType 兑换码类型
     * @param status 兑换码状态
     * @return 兑换码列表
     */
    @Select({
            "<script>",
            ModelEntitlementSql.REDEMPTION_CODE_SELECT_COLUMNS,
            ModelEntitlementSql.REDEMPTION_CODE_FILTER_SQL,
            ModelEntitlementSql.REDEMPTION_CODE_ORDER_SQL,
            "</script>"
    })
    List<RedemptionCodeRecord> findRedemptionsForExport(
            @Param("keyword") String keyword,
            @Param("codeType") String codeType,
            @Param("status") String status
    );

    /**
     * 按ID查询兑换码并加锁。
     *
     * @param id 兑换码ID
     * @return 兑换码记录
     */
    @Select("""
            SELECT id, code, code_type, status, used_by_user_id, used_at, created_at, updated_at, deleted
            FROM redemption_codes
            WHERE id = #{id} AND deleted = 0
            FOR UPDATE
            """)
    RedemptionCodeRecord findRedemptionByIdForUpdate(@Param("id") Long id);

    /**
     * 按兑换码查询并加锁。
     *
     * @param code 兑换码
     * @return 兑换码记录
     */
    @Select("""
            SELECT id, code, code_type, status, used_by_user_id, used_at, created_at, updated_at, deleted
            FROM redemption_codes
            WHERE BINARY code = #{code} AND deleted = 0
            FOR UPDATE
            """)
    RedemptionCodeRecord findRedemptionByCodeForUpdate(@Param("code") String code);

    /**
     * 判断兑换码是否存在。
     *
     * @param code 兑换码
     * @return 兑换码ID
     */
    @Select("SELECT id FROM redemption_codes WHERE BINARY code = #{code} LIMIT 1")
    Long findRedemptionIdByCode(@Param("code") String code);

    /**
     * 新增兑换码。
     *
     * @param code 兑换码
     * @param codeType 兑换码类型
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO redemption_codes(code, code_type, status)
            VALUES(#{code}, #{codeType}, 'UNUSED')
            """)
    int insertRedemptionCode(@Param("code") String code, @Param("codeType") String codeType);

    /**
     * 编辑未使用兑换码类型。
     *
     * @param id 兑换码ID
     * @param codeType 新类型
     * @return 影响行数
     */
    @Update("""
            UPDATE redemption_codes
            SET code_type = #{codeType}
            WHERE id = #{id} AND status = 'UNUSED' AND deleted = 0
            """)
    int updateUnusedRedemptionType(@Param("id") Long id, @Param("codeType") String codeType);

    /**
     * 删除未使用兑换码。
     *
     * @param id 兑换码ID
     * @return 影响行数
     */
    @Update("UPDATE redemption_codes SET deleted = 1 WHERE id = #{id} AND status = 'UNUSED' AND deleted = 0")
    int deleteUnusedRedemption(@Param("id") Long id);

    /**
     * 标记兑换码已使用。
     *
     * @param id 兑换码ID
     * @param userId 用户ID
     * @param usedAt 使用时间
     * @return 影响行数
     */
    @Update("""
            UPDATE redemption_codes
            SET status = 'USED',
                used_by_user_id = #{userId},
                used_at = #{usedAt}
            WHERE id = #{id} AND status = 'UNUSED' AND deleted = 0
            """)
    int markRedemptionUsed(@Param("id") Long id, @Param("userId") Long userId, @Param("usedAt") LocalDateTime usedAt);
}

/**
 * 模型权益 Mapper 复用 SQL 片段。
 */
final class ModelEntitlementSql {

    /** 兑换码列表通用查询字段。 */
    static final String REDEMPTION_CODE_SELECT_COLUMNS = """
            SELECT rc.id, rc.code, rc.code_type, rc.status, rc.used_by_user_id,
                   u.username AS used_by_username, rc.used_at, rc.created_at, rc.updated_at, rc.deleted
            """;

    /** 兑换码管理查询通用关联和筛选条件。 */
    static final String REDEMPTION_CODE_FILTER_SQL = """
            FROM redemption_codes rc
            LEFT JOIN users u ON u.id = rc.used_by_user_id
            WHERE rc.deleted = 0
            <if test='keyword != null and keyword != ""'>
              AND (rc.code LIKE CONCAT('%', #{keyword}, '%')
                   OR u.username LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='codeType != null and codeType != ""'>
              AND rc.code_type = #{codeType}
            </if>
            <if test='status != null and status != ""'>
              AND rc.status = #{status}
            </if>
            """;

    /** 兑换码列表和导出保持一致的排序规则。 */
    static final String REDEMPTION_CODE_ORDER_SQL = "ORDER BY rc.created_at DESC, rc.id DESC";

    /**
     * 工具类不允许实例化。
     */
    private ModelEntitlementSql() {
    }
}
