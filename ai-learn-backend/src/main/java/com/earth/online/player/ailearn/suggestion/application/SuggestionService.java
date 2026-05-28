package com.earth.online.player.ailearn.suggestion.application;

import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.security.AuthSupport;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.common.util.DateTimeUtils;
import com.earth.online.player.ailearn.common.util.PageRequestUtils;
import com.earth.online.player.ailearn.interaction.application.InteractionContentValidator;
import com.earth.online.player.ailearn.interaction.application.InteractionLikeToggler;
import com.earth.online.player.ailearn.interaction.application.InteractionTargetValidator;
import com.earth.online.player.ailearn.interaction.domain.AuthorSummary;
import com.earth.online.player.ailearn.interaction.domain.AuthorSummaryConverter;
import com.earth.online.player.ailearn.interaction.domain.InteractionSort;
import com.earth.online.player.ailearn.interaction.domain.InteractionTextPolicy;
import com.earth.online.player.ailearn.suggestion.domain.Suggestion;
import com.earth.online.player.ailearn.suggestion.domain.SuggestionType;
import com.earth.online.player.ailearn.suggestion.infrastructure.SuggestionMapper;
import com.earth.online.player.ailearn.suggestion.infrastructure.SuggestionRecord;
import com.earth.online.player.ailearn.suggestion.interfaces.CreateSuggestionRequest;
import com.earth.online.player.ailearn.suggestion.interfaces.SuggestionResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 建议应用服务。
 */
@Service
public class SuggestionService {

    private static final String CONTENT_NAME = "建议";
    private static final int DEFAULT_LIKE_COUNT = 0;

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
        int safePageNo = PageRequestUtils.normalizePageNo(pageNo);
        int safePageSize = PageRequestUtils.normalizePageSize(pageSize);
        int offset = PageRequestUtils.calculateOffset(safePageNo, safePageSize);
        InteractionSort safeSort = InteractionSort.from(sort);
        long viewerUserId = AuthSupport.resolveViewerUserId();

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
        AuthenticatedUser currentUser = AuthSupport.requireCurrentUser();
        String content = InteractionTextPolicy.normalize(request.content());
        InteractionContentValidator.validatePlainTextContent(content, CONTENT_NAME);

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
        AuthenticatedUser currentUser = AuthSupport.requireCurrentUser();
        ensureSuggestionExists(suggestionId);

        // INSERT IGNORE 成功代表点赞，失败代表之前已点赞，需要取消。
        InteractionLikeToggler.toggle(
                suggestionId,
                currentUser.userId(),
                suggestionMapper::insertLike,
                suggestionMapper::deleteLike,
                suggestionMapper::increaseLikeCount,
                suggestionMapper::decreaseLikeCount
        );
        return findOne(suggestionId, currentUser.userId());
    }

    /**
     * 确认建议存在。
     *
     * @param suggestionId 建议ID
     */
    private void ensureSuggestionExists(Long suggestionId) {
        InteractionTargetValidator.ensureExists(suggestionId, CONTENT_NAME, suggestionMapper::countActiveById);
    }

    /**
     * 查询单条建议响应。
     *
     * @param suggestionId 建议ID
     * @param viewerUserId 当前查看用户ID
     * @return 建议响应
     */
    private SuggestionResponse findOne(Long suggestionId, long viewerUserId) {
        SuggestionRecord record = InteractionTargetValidator.requireFound(
                suggestionMapper.findById(suggestionId, viewerUserId),
                CONTENT_NAME
        );
        return toResponse(record);
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
                DateTimeUtils.toOffsetDateTime(record.getCreatedAt())
        );
    }

    /**
     * 转换作者摘要。
     *
     * @param record 建议记录
     * @return 作者摘要
     */
    private AuthorSummary toAuthorSummary(SuggestionRecord record) {
        return AuthorSummaryConverter.toSummary(
                record.getAuthorId(),
                record.getAuthorUsername(),
                record.getAuthorNickname(),
                record.getAuthorAvatar(),
                record.getAuthorExperience()
        );
    }
}
