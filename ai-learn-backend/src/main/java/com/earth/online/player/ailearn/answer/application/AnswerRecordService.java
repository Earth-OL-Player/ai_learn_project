package com.earth.online.player.ailearn.answer.application;

import com.earth.online.player.ailearn.answer.infrastructure.AnswerRecordItemRecord;
import com.earth.online.player.ailearn.answer.infrastructure.AnswerRecordMapper;
import com.earth.online.player.ailearn.answer.interfaces.AnswerRecordResponse;
import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.question.domain.QuestionDifficulty;
import com.earth.online.player.ailearn.question.domain.QuestionType;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 答题记录应用服务。
 */
@Service
public class AnswerRecordService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final AnswerRecordMapper answerRecordMapper;

    /**
     * 创建答题记录服务。
     *
     * @param answerRecordMapper 答题记录仓储
     */
    public AnswerRecordService(AnswerRecordMapper answerRecordMapper) {
        this.answerRecordMapper = answerRecordMapper;
    }

    /**
     * 分页查询当前用户答题记录。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @return 分页记录
     */
    public PageResponse<AnswerRecordResponse> findCurrentUserPage(Integer pageNo, Integer pageSize) {
        Long userId = currentUserId();
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePageNo - 1) * safePageSize;

        // 答题记录只允许查询当前登录用户自己的数据。
        List<AnswerRecordResponse> records = answerRecordMapper.findPageByUser(userId, offset, safePageSize)
                .stream()
                .map(this::toResponse)
                .toList();
        long total = answerRecordMapper.countByUser(userId);
        return new PageResponse<>(records, safePageNo, safePageSize, total);
    }

    /**
     * 获取当前用户ID。
     *
     * @return 用户ID
     */
    private Long currentUserId() {
        AuthenticatedUser authenticatedUser = AuthContext.getUser();
        if (authenticatedUser == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录后即可使用该功能");
        }
        return authenticatedUser.userId();
    }

    /**
     * 转换响应对象。
     *
     * @param record 查询投影
     * @return 响应对象
     */
    private AnswerRecordResponse toResponse(AnswerRecordItemRecord record) {
        QuestionType questionType = QuestionType.valueOf(record.getQuestionType());
        QuestionDifficulty difficulty = QuestionDifficulty.valueOf(record.getDifficulty());
        return new AnswerRecordResponse(
                String.valueOf(record.getId()),
                String.valueOf(record.getQuestionId()),
                record.getQuestionTitle(),
                questionType.name(),
                questionType.text(),
                difficulty.name(),
                difficulty.text(),
                record.getScore(),
                Boolean.TRUE.equals(record.getCorrect()),
                record.getImprovementAdvice(),
                record.getDurationSeconds(),
                Boolean.TRUE.equals(record.getFirstAttempt()),
                record.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
        );
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
}
