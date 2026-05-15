package com.earth.online.player.ailearn.question.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.question.domain.QuestionDifficulty;
import com.earth.online.player.ailearn.question.domain.QuestionType;
import com.earth.online.player.ailearn.question.infrastructure.MyQuestionMapper;
import com.earth.online.player.ailearn.question.infrastructure.MyQuestionRecord;
import com.earth.online.player.ailearn.question.infrastructure.QuestionInsertRecord;
import com.earth.online.player.ailearn.question.interfaces.ImportMyQuestionsRequest;
import com.earth.online.player.ailearn.question.interfaces.ImportMyQuestionsResponse;
import com.earth.online.player.ailearn.question.interfaces.MyQuestionRequest;
import com.earth.online.player.ailearn.question.interfaces.QuestionDetailResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 个人题库应用服务。
 */
@Service
public class MyQuestionService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String SOURCE_TYPE_USER_UPLOAD = "USER_UPLOAD";
    private static final String IMPORT_MODE_APPEND = "APPEND";
    private static final String IMPORT_MODE_REPLACE = "REPLACE";

    private final MyQuestionMapper myQuestionMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建个人题库服务。
     *
     * @param myQuestionMapper 个人题库仓储
     * @param objectMapper JSON 序列化器
     */
    public MyQuestionService(MyQuestionMapper myQuestionMapper, ObjectMapper objectMapper) {
        this.myQuestionMapper = myQuestionMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询我的题库。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param keyword 关键词
     * @param difficulty 难度
     * @param questionType 题型
     * @return 分页题目
     */
    public PageResponse<QuestionDetailResponse> findPage(
            Integer pageNo,
            Integer pageSize,
            String keyword,
            String difficulty,
            String questionType) {
        Long userId = currentUserId();
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        String safeKeyword = normalizeKeyword(keyword);
        String safeDifficulty = normalizeDifficulty(difficulty);
        String safeQuestionType = normalizeQuestionType(questionType);
        int offset = (safePageNo - 1) * safePageSize;

        List<QuestionDetailResponse> records = myQuestionMapper
                .findPage(userId, safeKeyword, safeDifficulty, safeQuestionType, offset, safePageSize)
                .stream()
                .map(this::toResponse)
                .toList();
        long total = myQuestionMapper.countPage(userId, safeKeyword, safeDifficulty, safeQuestionType);
        return new PageResponse<>(records, safePageNo, safePageSize, total);
    }

    /**
     * 新增个人题目。
     *
     * @param request 题目请求
     * @return 题目详情
     */
    @Transactional
    public QuestionDetailResponse create(MyQuestionRequest request) {
        Long userId = currentUserId();
        QuestionInsertRecord record = buildQuestionRecord(userId, request);
        myQuestionMapper.insertQuestion(record);
        bindKnowledgePoints(record.getId(), request.knowledgePoints());
        return new QuestionDetailResponse(
                String.valueOf(record.getId()),
                record.getTitle(),
                record.getContent(),
                record.getQuestionType(),
                QuestionType.valueOf(record.getQuestionType()).text(),
                record.getDifficulty(),
                QuestionDifficulty.valueOf(record.getDifficulty()).text(),
                normalizeList(request.tags()),
                normalizeList(request.knowledgePoints()),
                record.getStandardAnswer(),
                record.getAnalysis(),
                java.time.OffsetDateTime.now()
        );
    }

    /**
     * 批量导入个人题库。
     *
     * @param request 导入请求
     * @return 导入结果
     */
    @Transactional
    public ImportMyQuestionsResponse importQuestions(ImportMyQuestionsRequest request) {
        Long userId = currentUserId();
        String mode = normalizeImportMode(request.mode());
        if (IMPORT_MODE_REPLACE.equals(mode)) {
            myQuestionMapper.deleteAllMine(userId);
        }

        // 批量导入保持一个事务，任意题目不合法时整体回滚。
        for (MyQuestionRequest question : request.questions()) {
            QuestionInsertRecord record = buildQuestionRecord(userId, question);
            myQuestionMapper.insertQuestion(record);
            bindKnowledgePoints(record.getId(), question.knowledgePoints());
        }
        return new ImportMyQuestionsResponse(request.questions().size(), mode);
    }

    /**
     * 删除个人题目。
     *
     * @param questionId 题目ID
     * @return 是否删除成功
     */
    @Transactional
    public boolean deleteMine(Long questionId) {
        if (questionId == null || questionId < 1) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "题目ID不合法");
        }
        int affected = myQuestionMapper.deleteMine(currentUserId(), questionId);
        if (affected == 0) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "个人题目不存在或无权删除");
        }
        return true;
    }

    /**
     * 构造题目新增对象。
     *
     * @param userId 用户ID
     * @param request 请求对象
     * @return 新增对象
     */
    private QuestionInsertRecord buildQuestionRecord(Long userId, MyQuestionRequest request) {
        QuestionInsertRecord record = new QuestionInsertRecord();
        record.setOwnerUserId(userId);
        record.setTitle(request.title().trim());
        record.setContent(request.content().trim());
        record.setQuestionType(normalizeQuestionType(request.questionType()));
        record.setDifficulty(normalizeDifficulty(request.difficulty()));
        record.setTags(toJson(normalizeList(request.tags())));
        record.setStandardAnswer(request.standardAnswer().trim());
        record.setAnalysis(StringUtils.hasText(request.analysis()) ? request.analysis().trim() : null);
        record.setSourceType(SOURCE_TYPE_USER_UPLOAD);
        return record;
    }

    /**
     * 绑定题目知识点。
     *
     * @param questionId 题目ID
     * @param knowledgePoints 知识点名称
     */
    private void bindKnowledgePoints(Long questionId, List<String> knowledgePoints) {
        for (String name : normalizeList(knowledgePoints)) {
            myQuestionMapper.insertKnowledgePoint(name, "用户个人题库导入的知识点");
            Long knowledgePointId = myQuestionMapper.findKnowledgePointIdByName(name);
            if (knowledgePointId != null) {
                myQuestionMapper.insertQuestionKnowledgePoint(questionId, knowledgePointId);
            }
        }
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
     * @return 题目详情响应
     */
    private QuestionDetailResponse toResponse(MyQuestionRecord record) {
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
     * 规整导入模式。
     *
     * @param mode 原始模式
     * @return 安全模式
     */
    private String normalizeImportMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return IMPORT_MODE_APPEND;
        }
        String safeMode = mode.trim().toUpperCase(java.util.Locale.ROOT);
        if (!IMPORT_MODE_APPEND.equals(safeMode) && !IMPORT_MODE_REPLACE.equals(safeMode)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "导入模式只支持 APPEND 或 REPLACE");
        }
        return safeMode;
    }

    /**
     * 规整字符串列表。
     *
     * @param values 原始列表
     * @return 去重列表
     */
    private List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                normalized.add(value.trim());
            }
        }
        return normalized.stream().limit(20).toList();
    }

    /**
     * 规整关键词。
     *
     * @param keyword 原始关键词
     * @return 安全关键词
     */
    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    /**
     * 规整难度。
     *
     * @param difficulty 原始难度
     * @return 难度编码
     */
    private String normalizeDifficulty(String difficulty) {
        try {
            return QuestionDifficulty.valueOf(difficulty.trim()).name();
        } catch (RuntimeException exception) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "题目难度不合法");
        }
    }

    /**
     * 规整题型。
     *
     * @param questionType 原始题型
     * @return 题型编码
     */
    private String normalizeQuestionType(String questionType) {
        try {
            return QuestionType.valueOf(questionType.trim()).name();
        } catch (RuntimeException exception) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "题型不合法");
        }
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

    /**
     * 序列化标签。
     *
     * @param values 标签列表
     * @return JSON 字符串
     */
    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ResponseCode.SYSTEM_ERROR.code(), "题目标签处理失败");
        }
    }

    /**
     * 解析标签 JSON。
     *
     * @param tags 标签JSON
     * @return 标签列表
     */
    private List<String> parseTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(tags, new com.fasterxml.jackson.core.type.TypeReference<>() { });
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
