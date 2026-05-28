package com.earth.online.player.ailearn.comment.application;

import com.earth.online.player.ailearn.comment.domain.Comment;
import com.earth.online.player.ailearn.comment.infrastructure.CommentMapper;
import com.earth.online.player.ailearn.comment.infrastructure.CommentRecord;
import com.earth.online.player.ailearn.comment.interfaces.CommentResponse;
import com.earth.online.player.ailearn.comment.interfaces.CreateCommentRequest;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthSupport;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.common.util.DateTimeUtils;
import com.earth.online.player.ailearn.common.util.NumberUtils;
import com.earth.online.player.ailearn.common.util.PageRequestUtils;
import com.earth.online.player.ailearn.interaction.application.InteractionContentValidator;
import com.earth.online.player.ailearn.interaction.application.InteractionLikeToggler;
import com.earth.online.player.ailearn.interaction.application.InteractionTargetValidator;
import com.earth.online.player.ailearn.interaction.domain.AuthorSummary;
import com.earth.online.player.ailearn.interaction.domain.AuthorSummaryConverter;
import com.earth.online.player.ailearn.interaction.domain.InteractionSort;
import com.earth.online.player.ailearn.interaction.domain.InteractionTextPolicy;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评论应用服务。
 */
@Service
public class CommentService {

    private static final String CONTENT_NAME = "评论";
    private static final int DEFAULT_LIKE_COUNT = 0;

    private final CommentMapper commentMapper;

    /**
     * 创建评论应用服务。
     *
     * @param commentMapper 评论仓储
     */
    public CommentService(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    /**
     * 分页查询父评论和一级子评论。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param sort 排序方式
     * @return 分页评论
     */
    public PageResponse<CommentResponse> findPage(Integer pageNo, Integer pageSize, String sort) {
        int safePageNo = PageRequestUtils.normalizePageNo(pageNo);
        int safePageSize = PageRequestUtils.normalizePageSize(pageSize);
        int offset = PageRequestUtils.calculateOffset(safePageNo, safePageSize);
        InteractionSort safeSort = InteractionSort.from(sort);
        long viewerUserId = AuthSupport.resolveViewerUserId();

        // 父评论分页，子评论批量查询，避免列表出现重复父评论。
        List<CommentRecord> parentRecords = commentMapper.findParentPage(offset, safePageSize, safeSort.name(), viewerUserId);
        Map<Long, List<CommentResponse>> childrenByParentId = findChildrenByParentId(parentRecords, viewerUserId);

        // 响应中直接带 children，前端无需再次请求回复列表。
        List<CommentResponse> records = parentRecords.stream()
                .map(record -> toResponse(record, childrenByParentId.getOrDefault(record.getId(), List.of())))
                .toList();
        return new PageResponse<>(records, safePageNo, safePageSize, commentMapper.countActiveParents());
    }

    /**
     * 创建当前登录用户的评论。
     *
     * @param request 发表评论请求
     * @return 新评论响应
     */
    @Transactional
    public CommentResponse create(CreateCommentRequest request) {
        AuthenticatedUser currentUser = AuthSupport.requireCurrentUser();
        String content = InteractionTextPolicy.normalize(request.content());
        InteractionContentValidator.validatePlainTextContent(content, CONTENT_NAME);
        validateParentComment(request.parentId());

        // 评论区仅支持纯文字和一级父子评论。
        Comment comment = new Comment();
        comment.setUserId(currentUser.userId());
        comment.setContent(content);
        comment.setParentId(request.parentId());
        comment.setLikeCount(DEFAULT_LIKE_COUNT);
        commentMapper.insert(comment);

        return findOne(comment.getId(), currentUser.userId());
    }

    /**
     * 切换评论点赞状态。
     *
     * @param commentId 评论ID
     * @return 最新评论响应
     */
    @Transactional
    public CommentResponse toggleLike(Long commentId) {
        AuthenticatedUser currentUser = AuthSupport.requireCurrentUser();
        ensureCommentExists(commentId);

        // INSERT IGNORE 成功代表点赞，失败代表之前已点赞，需要取消。
        InteractionLikeToggler.toggle(
                commentId,
                currentUser.userId(),
                commentMapper::insertLike,
                commentMapper::deleteLike,
                commentMapper::increaseLikeCount,
                commentMapper::decreaseLikeCount
        );
        return findOne(commentId, currentUser.userId());
    }

    /**
     * 批量查询并按父评论ID分组子评论。
     *
     * @param parentRecords 父评论记录
     * @param viewerUserId 当前查看用户ID
     * @return 父评论ID到子评论响应列表的映射
     */
    private Map<Long, List<CommentResponse>> findChildrenByParentId(List<CommentRecord> parentRecords, long viewerUserId) {
        List<Long> parentIds = parentRecords.stream().map(CommentRecord::getId).toList();
        if (parentIds.isEmpty()) {
            return Map.of();
        }

        // 子评论固定按发布时间正序展示，贴近对话阅读顺序。
        return commentMapper.findChildrenByParentIds(parentIds, viewerUserId).stream()
                .collect(Collectors.groupingBy(
                        CommentRecord::getParentId,
                        Collectors.mapping(record -> toResponse(record, List.of()), Collectors.toList())
                ));
    }

    /**
     * 校验评论父级只能是有效父评论。
     *
     * @param parentId 父评论ID
     */
    private void validateParentComment(Long parentId) {
        if (parentId == null) {
            return;
        }
        ensureCommentExists(parentId);

        // 本期只支持父子两级，避免出现孙级评论导致 UI 复杂化。
        if (commentMapper.findParentId(parentId) != null) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "仅支持回复父评论");
        }
    }

    /**
     * 确认评论存在。
     *
     * @param commentId 评论ID
     */
    private void ensureCommentExists(Long commentId) {
        InteractionTargetValidator.ensureExists(commentId, CONTENT_NAME, commentMapper::countActiveById);
    }

    /**
     * 查询单条评论响应。
     *
     * @param commentId 评论ID
     * @param viewerUserId 当前查看用户ID
     * @return 评论响应
     */
    private CommentResponse findOne(Long commentId, long viewerUserId) {
        CommentRecord record = InteractionTargetValidator.requireFound(
                commentMapper.findById(commentId, viewerUserId),
                CONTENT_NAME
        );
        return toResponse(record, List.of());
    }

    /**
     * 转换评论响应。
     *
     * @param record 查询投影
     * @param children 子评论列表
     * @return 响应对象
     */
    private CommentResponse toResponse(CommentRecord record, List<CommentResponse> children) {
        AuthorSummary author = toAuthorSummary(record);

        // 输出安全作者摘要，不包含密码哈希等敏感字段。
        return new CommentResponse(
                String.valueOf(record.getId()),
                record.getContent(),
                record.getParentId() == null ? null : String.valueOf(record.getParentId()),
                record.getLikeCount(),
                Boolean.TRUE.equals(record.getLiked()),
                NumberUtils.toIntOrZero(record.getReplyCount()),
                author,
                children,
                DateTimeUtils.toOffsetDateTime(record.getCreatedAt())
        );
    }

    /**
     * 转换作者摘要。
     *
     * @param record 评论记录
     * @return 作者摘要
     */
    private AuthorSummary toAuthorSummary(CommentRecord record) {
        return AuthorSummaryConverter.toSummary(
                record.getAuthorId(),
                record.getAuthorUsername(),
                record.getAuthorNickname(),
                record.getAuthorAvatar(),
                record.getAuthorExperience()
        );
    }
}
