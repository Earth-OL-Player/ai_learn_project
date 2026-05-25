package com.earth.online.player.ailearn.user.application;

import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.security.AuthSupport;
import com.earth.online.player.ailearn.common.util.PageRequestUtils;
import com.earth.online.player.ailearn.user.infrastructure.UserQuestionStatsMapper;
import com.earth.online.player.ailearn.user.infrastructure.UserQuestionStatsOverviewRecord;
import com.earth.online.player.ailearn.user.infrastructure.UserQuestionStatsRecord;
import com.earth.online.player.ailearn.user.infrastructure.UserQuestionTypeStatsRecord;
import com.earth.online.player.ailearn.user.interfaces.UserQuestionStatsItemResponse;
import com.earth.online.player.ailearn.user.interfaces.UserQuestionStatsOverviewResponse;
import com.earth.online.player.ailearn.user.interfaces.UserQuestionTypeStatsResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 用户刷题汇总只读服务。
 */
@Service
public class UserQuestionStatsService {

    private static final BigDecimal ZERO_SCORE = BigDecimal.ZERO;

    private final UserQuestionStatsMapper userQuestionStatsMapper;

    /**
     * 创建用户刷题汇总只读服务。
     *
     * @param userQuestionStatsMapper 用户刷题汇总仓储
     */
    public UserQuestionStatsService(UserQuestionStatsMapper userQuestionStatsMapper) {
        this.userQuestionStatsMapper = userQuestionStatsMapper;
    }

    /**
     * 分页查询当前用户刷题记录。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param keyword 题目关键词
     * @param questionType 题目类型
     * @return 分页刷题记录
     */
    public PageResponse<UserQuestionStatsItemResponse> findCurrentUserStatsPage(
            Integer pageNo,
            Integer pageSize,
            String keyword,
            String questionType
    ) {
        Long currentUserId = AuthSupport.requireCurrentUserId();
        int normalizedPageNo = PageRequestUtils.normalizePageNo(pageNo);
        int normalizedPageSize = PageRequestUtils.normalizePageSize(pageSize);
        int offset = PageRequestUtils.calculateOffset(normalizedPageNo, normalizedPageSize);
        String normalizedKeyword = normalizeOptional(keyword);
        String normalizedQuestionType = normalizeOptional(questionType);

        // 列表接口只返回汇总表字段和题目表题干、题型，不读取答案与答题明细。
        List<UserQuestionStatsItemResponse> records = userQuestionStatsMapper
                .findPage(currentUserId, normalizedKeyword, normalizedQuestionType, offset, normalizedPageSize)
                .stream()
                .map(UserQuestionStatsService::toItemResponse)
                .toList();
        long total = userQuestionStatsMapper.countPage(currentUserId, normalizedKeyword, normalizedQuestionType);
        return new PageResponse<>(records, normalizedPageNo, normalizedPageSize, total);
    }

    /**
     * 查询当前用户刷题记录概览。
     *
     * @return 刷题记录概览
     */
    public UserQuestionStatsOverviewResponse getCurrentUserStatsOverview() {
        Long currentUserId = AuthSupport.requireCurrentUserId();
        UserQuestionStatsOverviewRecord overview = userQuestionStatsMapper.findOverview(currentUserId);
        List<String> questionTypes = userQuestionStatsMapper.findQuestionTypes(currentUserId);
        List<UserQuestionTypeStatsResponse> typeStats = userQuestionStatsMapper.findTypeStats(currentUserId)
                .stream()
                .map(UserQuestionStatsService::toTypeStatsResponse)
                .toList();

        // 空数据时 MySQL 聚合仍会返回一行，所有 nullable 字段统一兜底。
        return new UserQuestionStatsOverviewResponse(
                safeLong(overview.getPracticedQuestionCount()),
                safeLong(overview.getTotalAnswerCount()),
                safeDecimal(overview.getAverageBestScore()),
                safeDecimal(overview.getAverageLastScore()),
                safeLong(overview.getWeakQuestionCount()),
                overview.getLastAnsweredAt(),
                questionTypes,
                typeStats
        );
    }

    /**
     * 转换刷题记录响应项。
     *
     * @param record 刷题汇总查询记录
     * @return 刷题记录响应项
     */
    private static UserQuestionStatsItemResponse toItemResponse(UserQuestionStatsRecord record) {
        return new UserQuestionStatsItemResponse(
                record.getQuestionCode(),
                record.getQuestion(),
                record.getQuestionType(),
                record.getAnswerCount(),
                record.getBestScore(),
                record.getLastScore(),
                record.getFirstAnsweredAt(),
                record.getLastAnsweredAt()
        );
    }

    /**
     * 转换题型汇总响应。
     *
     * @param record 题型汇总查询记录
     * @return 题型汇总响应
     */
    private static UserQuestionTypeStatsResponse toTypeStatsResponse(UserQuestionTypeStatsRecord record) {
        return new UserQuestionTypeStatsResponse(
                record.getQuestionType(),
                safeLong(record.getQuestionCount()),
                safeLong(record.getAnswerCount()),
                safeDecimal(record.getAverageBestScore()),
                safeDecimal(record.getAverageLastScore()),
                safeLong(record.getWeakCount())
        );
    }

    /**
     * 规整可选查询参数。
     *
     * @param value 原始参数
     * @return 规整后的参数
     */
    private static String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 安全转换长整型。
     *
     * @param value 原始数值
     * @return 非空长整型
     */
    private static long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 安全转换小数。
     *
     * @param value 原始数值
     * @return 非空小数
     */
    private static BigDecimal safeDecimal(BigDecimal value) {
        return value == null ? ZERO_SCORE : value;
    }
}
