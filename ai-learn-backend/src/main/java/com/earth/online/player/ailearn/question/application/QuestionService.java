package com.earth.online.player.ailearn.question.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.util.DateTimeUtils;
import com.earth.online.player.ailearn.common.util.IdRequestUtils;
import com.earth.online.player.ailearn.common.util.PageRequestUtils;
import com.earth.online.player.ailearn.common.util.TextUtils;
import com.earth.online.player.ailearn.question.infrastructure.QuestionDetailRecord;
import com.earth.online.player.ailearn.question.infrastructure.QuestionListRecord;
import com.earth.online.player.ailearn.question.infrastructure.QuestionMapper;
import com.earth.online.player.ailearn.question.interfaces.QuestionDetailResponse;
import com.earth.online.player.ailearn.question.interfaces.QuestionListResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 题库查询应用服务。
 */
@Service
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final QuestionTypeCache questionTypeCache;

    /**
     * 创建题库查询服务。
     *
     * @param questionMapper 题库仓储
     * @param questionTypeCache 题目分类缓存
     */
    public QuestionService(QuestionMapper questionMapper, QuestionTypeCache questionTypeCache) {
        this.questionMapper = questionMapper;
        this.questionTypeCache = questionTypeCache;
    }

    /**
     * 分页查询系统题库。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param keyword 关键词
     * @param questionType 题目分类
     * @return 分页题目
     */
    public PageResponse<QuestionListResponse> findPage(
            Integer pageNo,
            Integer pageSize,
            String keyword,
            String questionType) {
        int safePageNo = PageRequestUtils.normalizePageNo(pageNo);
        int safePageSize = PageRequestUtils.normalizePageSize(pageSize);
        String safeKeyword = TextUtils.trimToNull(keyword);
        String safeQuestionType = TextUtils.trimToNull(questionType);
        int offset = PageRequestUtils.calculateOffset(safePageNo, safePageSize);

        // 系统题库列表只展示未删除题目，避免引入额外来源维度。
        List<QuestionListResponse> records = questionMapper.findPage(
                        safeKeyword, safeQuestionType, offset, safePageSize)
                .stream()
                .map(this::toListResponse)
                .toList();
        long total = questionMapper.countPage(safeKeyword, safeQuestionType);
        return new PageResponse<>(records, safePageNo, safePageSize, total);
    }

    /**
     * 查询题目表中实际存在的分类。
     *
     * @return 题目分类列表
     */
    public List<String> findQuestionTypes() {
        return questionTypeCache.findQuestionTypes();
    }

    /**
     * 查询热门面经阅读文档题目。
     *
     * @param questionType 题目分类
     * @return 热门面经题目详情列表
     */
    public List<QuestionDetailResponse> findInterviewDocument(String questionType) {
        String safeQuestionType = TextUtils.trimToNull(questionType);

        // 阅读文档按当前分类查询，避免一次性加载全量题目造成页面卡顿。
        return questionMapper.findInterviewDocument(safeQuestionType)
                .stream()
                .map(this::toDetailResponse)
                .toList();
    }

    /**
     * 查询题目详情。
     *
     * @param id 题目ID
     * @return 题目详情
     */
    public QuestionDetailResponse findDetail(Long id) {
        Long safeId = IdRequestUtils.requirePositive(id, "题目ID不合法");
        QuestionDetailRecord record = questionMapper.findDetailById(safeId);
        if (record == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "题目不存在或已下架");
        }
        return toDetailResponse(record);
    }

    /**
     * 转换题目列表响应。
     *
     * @param record 查询投影
     * @return 列表响应
     */
    private QuestionListResponse toListResponse(QuestionListRecord record) {
        return new QuestionListResponse(
                String.valueOf(record.getId()),
                record.getCode(),
                record.getQuestion(),
                record.getQuestionType(),
                record.getQuestionType(),
                record.getImportanceScore(),
                record.getOccurrenceCount(),
                DateTimeUtils.toOffsetDateTime(record.getCreatedAt())
        );
    }

    /**
     * 转换题目详情响应。
     *
     * @param record 查询投影
     * @return 详情响应
     */
    private QuestionDetailResponse toDetailResponse(QuestionDetailRecord record) {
        return new QuestionDetailResponse(
                String.valueOf(record.getId()),
                record.getCode(),
                record.getQuestion(),
                record.getQuestionType(),
                record.getQuestionType(),
                record.getStandardAnswer(),
                record.getImportanceScore(),
                record.getOccurrenceCount(),
                DateTimeUtils.toOffsetDateTime(record.getCreatedAt())
        );
    }

}
