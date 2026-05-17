package com.earth.online.player.ailearn.comment.application;

import com.earth.online.player.ailearn.comment.domain.Comment;
import com.earth.online.player.ailearn.comment.infrastructure.CommentMapper;
import com.earth.online.player.ailearn.comment.infrastructure.CommentRecord;
import com.earth.online.player.ailearn.comment.interfaces.CommentResponse;
import com.earth.online.player.ailearn.comment.interfaces.CreateCommentRequest;
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
import java.time.ZoneId;
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

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_LIKE_COUNT = 0;
    private static final long ANONYMOUS_USER_ID = 0L;

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
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        int offset = calculateOffset(safePageNo, safePageSize);
        InteractionSort safeSort = InteractionSort.from(sort);
        long viewerUserId = resolveViewerUserId();

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
        AuthenticatedUser currentUser = requireCurrentUser();
        String content = InteractionTextPolicy.normalize(request.content());
        validateContent(content);
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
        AuthenticatedUser currentUser = requireCurrentUser();
        ensureCommentExists(commentId);

        // INSERT IGNORE 成功代表点赞，失败代表之前已点赞，需要取消。
        int insertedRows = commentMapper.insertLike(commentId, currentUser.userId());
        if (insertedRows > 0) {
            commentMapper.increaseLikeCount(commentId);
        } else if (commentMapper.deleteLike(commentId, currentUser.userId()) > 0) {
            commentMapper.decreaseLikeCount(commentId);
        }
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
     * 校验评论内容。
     *
     * @param content 已规整内容
     */
    private void validateContent(String content) {
        if (InteractionTextPolicy.hasInvalidLength(content)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "评论内容长度需在2到1000位之间");
        }
        if (InteractionTextPolicy.containsUnsupportedContent(content)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "仅支持纯文字，不能使用表情和艾特");
        }
    }

    /**
     * 确认评论存在。
     *
     * @param commentId 评论ID
     */
    private void ensureCommentExists(Long commentId) {
        if (commentId == null || commentMapper.countActiveById(commentId) == 0) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "评论不存在");
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
     * 查询单条评论响应。
     *
     * @param commentId 评论ID
     * @param viewerUserId 当前查看用户ID
     * @return 评论响应
     */
    private CommentResponse findOne(Long commentId, long viewerUserId) {
        CommentRecord record = commentMapper.findById(commentId, viewerUserId);
        if (record == null) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "评论不存在");
        }
        return toResponse(record, List.of());
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
                record.getReplyCount() == null ? 0 : record.getReplyCount(),
                author,
                children,
                record.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
        );
    }

    /**
     * 转换作者摘要。
     *
     * @param record 评论记录
     * @return 作者摘要
     */
    private AuthorSummary toAuthorSummary(CommentRecord record) {
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
