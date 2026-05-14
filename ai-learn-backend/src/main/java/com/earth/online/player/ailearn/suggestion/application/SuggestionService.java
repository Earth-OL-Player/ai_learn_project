package com.earth.online.player.ailearn.suggestion.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.interaction.domain.AuthorSummary;
import com.earth.online.player.ailearn.suggestion.domain.Suggestion;
import com.earth.online.player.ailearn.suggestion.domain.SuggestionStatus;
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
     * @return 分页建议
     */
    public PageResponse<SuggestionResponse> findPage(Integer pageNo, Integer pageSize) {
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        int offset = calculateOffset(safePageNo, safePageSize);

        // 列表只返回未删除数据，并按创建时间倒序展示。
        List<SuggestionResponse> records = suggestionMapper.findPage(offset, safePageSize).stream()
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
        AuthenticatedUser currentUser = AuthContext.getUser();
        if (currentUser == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录后即可使用该功能");
        }

        String title = request.title().trim();
        String content = request.content().trim();
        validateTrimmedContent(title, content);

        // 只允许写入枚举定义内的类型，状态固定为待处理。
        Suggestion suggestion = new Suggestion();
        suggestion.setUserId(currentUser.userId());
        suggestion.setTitle(title);
        suggestion.setContent(content);
        suggestion.setType(SuggestionType.valueOf(request.type()).name());
        suggestion.setStatus(SuggestionStatus.PENDING.name());
        suggestionMapper.insert(suggestion);

        // 创建接口返回最小安全信息，列表刷新后可看到完整创建时间。
        AuthorSummary author = new AuthorSummary(String.valueOf(currentUser.userId()), currentUser.username(), null, null);
        return new SuggestionResponse(
                String.valueOf(suggestion.getId()),
                suggestion.getTitle(),
                suggestion.getContent(),
                suggestion.getType(),
                SuggestionType.valueOf(suggestion.getType()).text(),
                suggestion.getStatus(),
                SuggestionStatus.PENDING.text(),
                author,
                null
        );
    }

    /**
     * 校验去除首尾空格后的建议内容。
     *
     * @param title 标题
     * @param content 内容
     */
    private void validateTrimmedContent(String title, String content) {
        if (title.length() < 2 || title.length() > 80) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "建议标题长度需在2到80位之间");
        }
        if (content.length() < 5 || content.length() > 2000) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "建议内容长度需在5到2000位之间");
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
     * 转换建议列表响应。
     *
     * @param record 查询投影
     * @return 响应对象
     */
    private SuggestionResponse toResponse(SuggestionRecord record) {
        SuggestionType type = SuggestionType.valueOf(record.getType());
        SuggestionStatus status = SuggestionStatus.valueOf(record.getStatus());
        AuthorSummary author = new AuthorSummary(
                String.valueOf(record.getAuthorId()),
                record.getAuthorUsername(),
                record.getAuthorNickname(),
                record.getAuthorAvatar()
        );

        // 统一转换为带时区偏移的时间，便于前端本地化展示。
        return new SuggestionResponse(
                String.valueOf(record.getId()),
                record.getTitle(),
                record.getContent(),
                type.name(),
                type.text(),
                status.name(),
                status.text(),
                author,
                record.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
        );
    }
}
