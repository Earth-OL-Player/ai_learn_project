package com.earth.online.player.ailearn.question.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.question.domain.QuestionDifficulty;
import com.earth.online.player.ailearn.question.domain.QuestionType;
import com.earth.online.player.ailearn.question.infrastructure.QuestionDetailRecord;
import com.earth.online.player.ailearn.question.infrastructure.QuestionListRecord;
import com.earth.online.player.ailearn.question.infrastructure.QuestionMapper;
import com.earth.online.player.ailearn.question.interfaces.QuestionDetailResponse;
import com.earth.online.player.ailearn.question.interfaces.QuestionListResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
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
    private final ObjectMapper objectMapper;

    /**
     * 创建题库查询服务。
     *
     * @param questionMapper 题库仓储
     * @param objectMapper JSON 解析器
     */
    public QuestionService(QuestionMapper questionMapper, ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询默认题库。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param keyword 关键词
     * @param difficulty 难度
     * @param questionType 题型
     * @param knowledgePointId 知识点ID
     * @return 分页题目
     */
    public PageResponse<QuestionListResponse> findPage(
            Integer pageNo,
            Integer pageSize,
            String keyword,
            String difficulty,
            String questionType,
            Long knowledgePointId) {
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        String safeKeyword = normalizeKeyword(keyword);
        String safeDifficulty = normalizeDifficulty(difficulty);
        String safeQuestionType = normalizeQuestionType(questionType);
        int offset = calculateOffset(safePageNo, safePageSize);

        // 默认题库只展示未删除且来源为 DEFAULT 的题目。
        List<QuestionListResponse> records = questionMapper.findPage(
                        safeKeyword, safeDifficulty, safeQuestionType, knowledgePointId, offset, safePageSize)
                .stream()
                .map(this::toListResponse)
                .toList();
        long total = questionMapper.countPage(safeKeyword, safeDifficulty, safeQuestionType, knowledgePointId);
        return new PageResponse<>(records, safePageNo, safePageSize, total);
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
     * 规整难度筛选。
     *
     * @param difficulty 原始难度
     * @return 安全难度
     */
    private String normalizeDifficulty(String difficulty) {
        if (!StringUtils.hasText(difficulty)) {
            return null;
        }
        try {
            return QuestionDifficulty.valueOf(difficulty.trim()).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "题目难度不合法");
        }
    }

    /**
     * 规整题型筛选。
     *
     * @param questionType 原始题型
     * @return 安全题型
     */
    private String normalizeQuestionType(String questionType) {
        if (!StringUtils.hasText(questionType)) {
            return null;
        }
        try {
            return QuestionType.valueOf(questionType.trim()).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "题型不合法");
        }
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
        QuestionType questionType = QuestionType.valueOf(record.getQuestionType());
        QuestionDifficulty difficulty = QuestionDifficulty.valueOf(record.getDifficulty());
        return new QuestionListResponse(
                String.valueOf(record.getId()),
                record.getTitle(),
                questionType.name(),
                questionType.text(),
                difficulty.name(),
                difficulty.text(),
                parseTags(record.getTags()),
                splitNames(record.getKnowledgePointNames()),
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
        QuestionType questionType = QuestionType.valueOf(record.getQuestionType());
        QuestionDifficulty difficulty = QuestionDifficulty.valueOf(record.getDifficulty());
        return new QuestionDetailResponse(
                String.valueOf(record.getId()),
                record.getTitle(),
                record.getContent(),
                questionType.name(),
                questionType.text(),
                difficulty.name(),
                difficulty.text(),
                parseTags(record.getTags()),
                splitNames(record.getKnowledgePointNames()),
                record.getStandardAnswer(),
                record.getAnalysis(),
                record.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
        );
    }

    /**
     * 解析标签 JSON 数组。
     *
     * @param tags 标签 JSON
     * @return 标签列表
     */
    private List<String> parseTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(tags, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ResponseCode.SYSTEM_ERROR.code(), "题目标签解析失败");
        }
    }

    /**
     * 拆分知识点名称。
     *
     * @param names 聚合名称
     * @return 名称列表
     */
    private List<String> splitNames(String names) {
        if (!StringUtils.hasText(names)) {
            return Collections.emptyList();
        }
        return Arrays.stream(names.split(","))
                .filter(StringUtils::hasText)
                .toList();
    }
}
