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
import com.earth.online.player.ailearn.interaction.domain.AuthorSummary;
import java.time.ZoneId;
import java.util.List;
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
     * 分页查询评论列表。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @return 分页评论
     */
    public PageResponse<CommentResponse> findPage(Integer pageNo, Integer pageSize) {
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        int offset = calculateOffset(safePageNo, safePageSize);

        // 评论列表按发布时间倒序展示，回复树能力仅预留字段。
        List<CommentResponse> records = commentMapper.findPage(offset, safePageSize).stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(records, safePageNo, safePageSize, commentMapper.countActive());
    }

    /**
     * 创建当前登录用户的评论。
     *
     * @param request 发表评论请求
     * @return 新评论响应
     */
    @Transactional
    public CommentResponse create(CreateCommentRequest request) {
        AuthenticatedUser currentUser = AuthContext.getUser();
        if (currentUser == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录后即可使用该功能");
        }

        String content = request.content().trim();
        validateTrimmedContent(content);

        // 本期前端不传父评论，后端保留 parentId 扩展点。
        Comment comment = new Comment();
        comment.setUserId(currentUser.userId());
        comment.setContent(content);
        comment.setParentId(request.parentId());
        comment.setLikeCount(DEFAULT_LIKE_COUNT);
        commentMapper.insert(comment);

        AuthorSummary author = new AuthorSummary(String.valueOf(currentUser.userId()), currentUser.username(), null, null);
        return new CommentResponse(
                String.valueOf(comment.getId()),
                comment.getContent(),
                comment.getParentId() == null ? null : String.valueOf(comment.getParentId()),
                DEFAULT_LIKE_COUNT,
                author,
                null
        );
    }

    /**
     * 校验去除首尾空格后的评论内容。
     *
     * @param content 评论内容
     */
    private void validateTrimmedContent(String content) {
        if (content.length() < 2 || content.length() > 1000) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "评论内容长度需在2到1000位之间");
        }
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
     * 转换评论列表响应。
     *
     * @param record 查询投影
     * @return 响应对象
     */
    private CommentResponse toResponse(CommentRecord record) {
        AuthorSummary author = new AuthorSummary(
                String.valueOf(record.getAuthorId()),
                record.getAuthorUsername(),
                record.getAuthorNickname(),
                record.getAuthorAvatar()
        );

        // 输出安全作者摘要，不包含密码哈希等敏感字段。
        return new CommentResponse(
                String.valueOf(record.getId()),
                record.getContent(),
                record.getParentId() == null ? null : String.valueOf(record.getParentId()),
                record.getLikeCount(),
                author,
                record.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
        );
    }
}
