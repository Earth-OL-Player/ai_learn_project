package com.earth.online.player.ailearn.suggestion.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.growth.domain.GrowthLevel;
import com.earth.online.player.ailearn.growth.domain.GrowthRank;
import com.earth.online.player.ailearn.interaction.domain.AuthorSummary;
import com.earth.online.player.ailearn.interaction.domain.InteractionSort;
import com.earth.online.player.ailearn.interaction.domain.InteractionTextPolicy;
import com.earth.online.player.ailearn.suggestion.domain.Suggestion;
import com.earth.online.player.ailearn.suggestion.domain.SuggestionType;
import com.earth.online.player.ailearn.suggestion.infrastructure.SuggestionMapper;
import com.earth.online.player.ailearn.suggestion.infrastructure.SuggestionRecord;
import com.earth.online.player.ailearn.suggestion.interfaces.CreateSuggestionRequest;
import com.earth.online.player.ailearn.suggestion.interfaces.SuggestionResponse;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 建议应用服务。
 */
@Service
public class SuggestionService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_LIKE_COUNT = 0;
    private static final long ANONYMOUS_USER_ID = 0L;

    private final SuggestionMapper suggestionMapper;

    /**
     * 创建建议应用服务。
     *
     * @param suggestionMapper 建议仓储
     */
    public SuggestionService(SuggestionMapper suggestionMapper) {
        this.suggestionMapper = suggestionMapper;
    }

    /**
     * 分页查询建议列表。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param sort 排序方式
     * @return 分页建议
     */
    public PageResponse<SuggestionResponse> findPage(Integer pageNo, Integer pageSize, String sort) {
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        int offset = calculateOffset(safePageNo, safePageSize);
        InteractionSort safeSort = InteractionSort.from(sort);
        long viewerUserId = resolveViewerUserId();

        // 建议区不支持父子结构，直接分页返回评论流卡片数据。
        List<SuggestionResponse> records = suggestionMapper.findPage(offset, safePageSize, safeSort.name(), viewerUserId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(records, safePageNo, safePageSize, suggestionMapper.countActive());
    }

    /**
     * 创建当前登录用户的建议。
     *
     * @param request 提交建议请求
     * @return 新建议响应
     */
    @Transactional
    public SuggestionResponse create(CreateSuggestionRequest request) {
        AuthenticatedUser currentUser = requireCurrentUser();
        String content = InteractionTextPolicy.normalize(request.content());
        validateContent(content);

        // 建议类型仅保留本期明确要求的四类。
        Suggestion suggestion = new Suggestion();
        suggestion.setUserId(currentUser.userId());
        suggestion.setContent(content);
        suggestion.setType(SuggestionType.valueOf(request.type()).name());
        suggestion.setLikeCount(DEFAULT_LIKE_COUNT);
        suggestionMapper.insert(suggestion);

        return findOne(suggestion.getId(), currentUser.userId());
    }

    /**
     * 切换建议点赞状态。
     *
     * @param suggestionId 建议ID
     * @return 最新建议响应
     */
    @Transactional
    public SuggestionResponse toggleLike(Long suggestionId) {
        AuthenticatedUser currentUser = requireCurrentUser();
        ensureSuggestionExists(suggestionId);

        // INSERT IGNORE 成功代表点赞，失败代表之前已点赞，需要取消。
        int insertedRows = suggestionMapper.insertLike(suggestionId, currentUser.userId());
        if (insertedRows > 0) {
            suggestionMapper.increaseLikeCount(suggestionId);
        } else if (suggestionMapper.deleteLike(suggestionId, currentUser.userId()) > 0) {
            suggestionMapper.decreaseLikeCount(suggestionId);
        }
        return findOne(suggestionId, currentUser.userId());
    }

    /**
     * 校验建议内容。
     *
     * @param content 已规整内容
     */
    private void validateContent(String content) {
        if (InteractionTextPolicy.hasInvalidLength(content)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "建议内容长度需在2到1000位之间");
        }
        if (InteractionTextPolicy.containsUnsupportedContent(content)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "仅支持纯文字，不能使用表情和艾特");
        }
    }

    /**
     * 确认建议存在。
     *
     * @param suggestionId 建议ID
     */
    private void ensureSuggestionExists(Long suggestionId) {
        if (suggestionId == null || suggestionMapper.countActiveById(suggestionId) == 0) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "建议不存在");
        }
    }

    /**
     * 获取当前登录用户。
     *
     * @return 当前登录用户
     */
    private AuthenticatedUser requireCurrentUser() {
        AuthenticatedUser currentUser = AuthContext.getUser();
        if (currentUser == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录后即可使用该功能");
        }
        return currentUser;
    }

    /**
     * 获取当前查看用户ID。
     *
     * @return 当前查看用户ID，未登录时返回0
     */
    private long resolveViewerUserId() {
        AuthenticatedUser currentUser = AuthContext.getUser();
        return currentUser == null ? ANONYMOUS_USER_ID : currentUser.userId();
    }

    /**
     * 查询单条建议响应。
     *
     * @param suggestionId 建议ID
     * @param viewerUserId 当前查看用户ID
     * @return 建议响应
     */
    private SuggestionResponse findOne(Long suggestionId, long viewerUserId) {
        SuggestionRecord record = suggestionMapper.findById(suggestionId, viewerUserId);
        if (record == null) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "建议不存在");
        }
        return toResponse(record);
    }

    /**
     * 规整页码。
     *
     * @param pageNo 原始页码
     * @return 安全页码
     */
    private int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo < DEFAULT_PAGE_NO) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo;
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
     * 计算分页偏移量，避免极端页码导致整数溢出。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @return 偏移量
     */
    private int calculateOffset(int pageNo, int pageSize) {
        long offset = (long) (pageNo - 1) * pageSize;
        return offset > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) offset;
    }

    /**
     * 转换建议列表响应。
     *
     * @param record 查询投影
     * @return 响应对象
     */
    private SuggestionResponse toResponse(SuggestionRecord record) {
        SuggestionType type = SuggestionType.valueOf(record.getType());
        AuthorSummary author = toAuthorSummary(record);

        // 响应不再返回处理状态，建议类型作为唯一业务标签展示。
        return new SuggestionResponse(
                String.valueOf(record.getId()),
                record.getContent(),
                type.name(),
                type.text(),
                record.getLikeCount(),
                Boolean.TRUE.equals(record.getLiked()),
                author,
                record.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
        );
    }

    /**
     * 转换作者摘要。
     *
     * @param record 建议记录
     * @return 作者摘要
     */
    private AuthorSummary toAuthorSummary(SuggestionRecord record) {
        int experience = record.getAuthorExperience() == null ? 0 : record.getAuthorExperience();
        GrowthLevel level = GrowthLevel.resolveByExperience(experience);
        GrowthRank rank = GrowthRank.resolveByExperience(experience);

        // 等级和段位遵循个人中心同一套成长规则。
        return new AuthorSummary(
                String.valueOf(record.getAuthorId()),
                record.getAuthorUsername(),
                record.getAuthorNickname(),
                record.getAuthorAvatar(),
                level.displayCode(),
                level.levelValue(),
                rank.displayName()
        );
    }
}
