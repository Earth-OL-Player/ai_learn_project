package com.earth.online.player.ailearn.question.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.question.infrastructure.QuestionDetailRecord;
import com.earth.online.player.ailearn.question.infrastructure.QuestionListRecord;
import com.earth.online.player.ailearn.question.infrastructure.QuestionMapper;
import com.earth.online.player.ailearn.question.interfaces.QuestionDetailResponse;
import com.earth.online.player.ailearn.question.interfaces.QuestionListResponse;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 题库查询应用服务。
 */
@Service
public class QuestionService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final QuestionMapper questionMapper;

    /**
     * 创建题库查询服务。
     *
     * @param questionMapper 题库仓储
     */
    public QuestionService(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
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
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        String safeKeyword = normalizeKeyword(keyword);
        String safeQuestionType = normalizeQuestionType(questionType);
        int offset = calculateOffset(safePageNo, safePageSize);

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
        return questionMapper.findQuestionTypes();
    }

    /**
     * 查询热门面经阅读文档题目。
     *
     * @return 热门面经题目详情列表
     */
    public List<QuestionDetailResponse> findInterviewDocument() {
        // 阅读文档需要一次性拿到参考答案，避免前端逐题打开详情弹窗。
        return questionMapper.findInterviewDocument()
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
        if (id == null || id < 1) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "题目ID不合法");
        }
        QuestionDetailRecord record = questionMapper.findDetailById(id);
        if (record == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "题目不存在或已下架");
        }
        return toDetailResponse(record);
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
     * 规整关键词。
     *
     * @param keyword 原始关键词
     * @return 安全关键词
     */
    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim();
    }

    /**
     * 规整题目分类。
     *
     * @param questionType 原始分类
     * @return 安全分类
     */
    private String normalizeQuestionType(String questionType) {
        if (!StringUtils.hasText(questionType)) {
            return null;
        }
        return questionType.trim();
    }

    /**
     * 计算分页偏移量。
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
                record.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
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
                record.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
        );
    }

}
