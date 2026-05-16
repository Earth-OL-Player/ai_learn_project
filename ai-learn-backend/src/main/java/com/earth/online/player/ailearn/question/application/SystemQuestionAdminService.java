package com.earth.online.player.ailearn.question.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.question.infrastructure.SystemQuestionAdminMapper;
import com.earth.online.player.ailearn.question.infrastructure.SystemQuestionRecord;
import com.earth.online.player.ailearn.question.infrastructure.SystemQuestionWriteRecord;
import com.earth.online.player.ailearn.question.interfaces.admin.ImportSystemQuestionsResponse;
import com.earth.online.player.ailearn.question.interfaces.admin.SystemQuestionRequest;
import com.earth.online.player.ailearn.question.interfaces.admin.SystemQuestionResponse;
import com.earth.online.player.ailearn.user.domain.User;
import com.earth.online.player.ailearn.user.infrastructure.UserMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 系统题库管理应用服务。
 */
@Service
public class SystemQuestionAdminService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_IMPORT_ROWS = 1000;
    private static final long MAX_IMPORT_FILE_SIZE = 2 * 1024 * 1024L;
    private static final int DEFAULT_IMPORTANCE_SCORE = 60;
    private static final int DEFAULT_OCCURRENCE_COUNT = 0;
    private static final String CSV_HEADER = "code,question,question_type,standard_answer,importance_score,occurrence_count\n";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserMapper userMapper;
    private final SystemQuestionAdminMapper systemQuestionAdminMapper;

    /**
     * 创建系统题库管理服务。
     *
     * @param userMapper 用户仓储
     * @param systemQuestionAdminMapper 系统题库仓储
     */
    public SystemQuestionAdminService(UserMapper userMapper, SystemQuestionAdminMapper systemQuestionAdminMapper) {
        this.userMapper = userMapper;
        this.systemQuestionAdminMapper = systemQuestionAdminMapper;
    }

    /**
     * 分页查询系统题库。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param keyword 关键词
     * @param questionType 题目分类
     * @return 分页结果
     */
    public PageResponse<SystemQuestionResponse> findPage(
            Integer pageNo,
            Integer pageSize,
            String keyword,
            String questionType) {
        requireSuperAdmin();
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        String safeKeyword = trimToNull(keyword);
        String safeQuestionType = trimToNull(questionType);
        int offset = (safePageNo - 1) * safePageSize;

        // 系统题库列表只返回未删除题目，避免误展示历史版本。
        List<SystemQuestionResponse> records = systemQuestionAdminMapper
                .findPage(safeKeyword, safeQuestionType, offset, safePageSize)
                .stream()
                .map(this::toResponse)
                .toList();
        long total = systemQuestionAdminMapper.countPage(safeKeyword, safeQuestionType);
        return new PageResponse<>(records, safePageNo, safePageSize, total);
    }

    /**
     * 查询系统题目详情。
     *
     * @param id 题目ID
     * @return 题目详情
     */
    public SystemQuestionResponse findDetail(Long id) {
        requireSuperAdmin();
        SystemQuestionRecord record = findExisting(id);
        return toResponse(record);
    }

    /**
     * 新增系统题目。
     *
     * @param request 保存请求
     * @return 保存后的题目
     */
    @Transactional
    public SystemQuestionResponse create(SystemQuestionRequest request) {
        requireSuperAdmin();
        SystemQuestionWriteRecord writeRecord = buildWriteRecord(request, null);
        SystemQuestionRecord sameCode = systemQuestionAdminMapper.findByCodeAny(writeRecord.getCode());
        if (sameCode != null && !Boolean.TRUE.equals(sameCode.getDeleted())) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "题目编码已存在");
        }

        // 已删除同编码题目直接恢复更新，保证 code 作为稳定业务标识。
        if (sameCode != null) {
            writeRecord.setId(sameCode.getId());
            systemQuestionAdminMapper.update(writeRecord);
            return toResponse(systemQuestionAdminMapper.findById(sameCode.getId()));
        }
        systemQuestionAdminMapper.insert(writeRecord);
        return toResponse(systemQuestionAdminMapper.findById(writeRecord.getId()));
    }

    /**
     * 更新系统题目。
     *
     * @param id 题目ID
     * @param request 保存请求
     * @return 更新后的题目
     */
    @Transactional
    public SystemQuestionResponse update(Long id, SystemQuestionRequest request) {
        requireSuperAdmin();
        findExisting(id);
        SystemQuestionWriteRecord writeRecord = buildWriteRecord(request, id);
        SystemQuestionRecord sameCode = systemQuestionAdminMapper.findByCodeAny(writeRecord.getCode());
        if (sameCode != null && !sameCode.getId().equals(id)) {
            throw new BusinessException(ResponseCode.RESOURCE_CONFLICT.code(), "题目编码已被其他题目使用");
        }

        // 编辑时同步写入新旧字段，保证历史页面和新功能都可读取。
        systemQuestionAdminMapper.update(writeRecord);
        return toResponse(systemQuestionAdminMapper.findById(id));
    }

    /**
     * 删除系统题目。
     *
     * @param id 题目ID
     * @return 是否删除成功
     */
    @Transactional
    public boolean delete(Long id) {
        requireSuperAdmin();
        if (id == null || id < 1) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "题目ID不合法");
        }
        int affected = systemQuestionAdminMapper.deleteById(id);
        if (affected == 0) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "系统题目不存在或已删除");
        }
        return true;
    }

    /**
     * 查询系统题目分类。
     *
     * @return 分类列表
     */
    public List<String> findQuestionTypes() {
        requireSuperAdmin();
        return systemQuestionAdminMapper.findQuestionTypes();
    }

    /**
     * 生成 CSV 模板内容。
     *
     * @return UTF-8 CSV 模板字节
     */
    public byte[] buildTemplate() {
        requireSuperAdmin();
        String example = "SYSTEM-RAG-001,什么是 RAG？,RAG,检索增强生成通过外部知识提升回答质量。,90,120\n";
        return ("\uFEFF" + CSV_HEADER + example).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 导入系统题库 CSV。
     *
     * @param file CSV 文件
     * @return 导入结果
     */
    @Transactional
    public ImportSystemQuestionsResponse importCsv(MultipartFile file) {
        requireSuperAdmin();
        validateImportFile(file);
        List<SystemQuestionRequest> requests = parseCsv(file);
        int createdCount = 0;
        int updatedCount = 0;

        // CSV 导入按 code 执行新增或更新，便于管理员批量维护题库。
        for (SystemQuestionRequest request : requests) {
            SystemQuestionWriteRecord writeRecord = buildWriteRecord(request, null);
            SystemQuestionRecord existing = systemQuestionAdminMapper.findByCodeAny(writeRecord.getCode());
            if (existing == null) {
                systemQuestionAdminMapper.insert(writeRecord);
                createdCount++;
            } else {
                writeRecord.setId(existing.getId());
                systemQuestionAdminMapper.update(writeRecord);
                updatedCount++;
            }
        }
        return new ImportSystemQuestionsResponse(requests.size(), createdCount, updatedCount);
    }

    /**
     * 校验当前用户必须是超级管理员。
     */
    private void requireSuperAdmin() {
        AuthenticatedUser authenticatedUser = AuthContext.getUser();
        if (authenticatedUser == null) {
            throw new BusinessException(ResponseCode.AUTH_UNAUTHORIZED.code(), "登录后即可使用该功能");
        }
        User user = userMapper.findById(authenticatedUser.userId());
        if (user == null || !Boolean.TRUE.equals(user.getSuperAdmin())) {
            throw new BusinessException(ResponseCode.AUTH_FORBIDDEN.code(), "仅超级管理员可维护系统题库");
        }
    }

    /**
     * 查询未删除题目。
     *
     * @param id 题目ID
     * @return 题目记录
     */
    private SystemQuestionRecord findExisting(Long id) {
        if (id == null || id < 1) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "题目ID不合法");
        }
        SystemQuestionRecord record = systemQuestionAdminMapper.findById(id);
        if (record == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "系统题目不存在或已删除");
        }
        return record;
    }

    /**
     * 构造写入记录。
     *
     * @param request 请求对象
     * @param id 题目ID
     * @return 写入记录
     */
    private SystemQuestionWriteRecord buildWriteRecord(SystemQuestionRequest request, Long id) {
        if (request == null) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "题目内容不能为空");
        }
        String question = requireText(request.question(), "题目不能为空");
        String questionType = normalizeQuestionType(request.questionType());
        String standardAnswer = requireText(request.standardAnswer(), "参考答案不能为空");
        String code = normalizeCode(request.code());

        // 兼容旧字段：标题取题目前120字符，内容与新 question 字段保持一致。
        SystemQuestionWriteRecord record = new SystemQuestionWriteRecord();
        record.setId(id);
        record.setCode(StringUtils.hasText(code) ? code : generateQuestionCode());
        record.setQuestion(question);
        record.setQuestionType(questionType);
        record.setStandardAnswer(standardAnswer);
        record.setImportanceScore(normalizeImportanceScore(request.importanceScore()));
        record.setOccurrenceCount(normalizeOccurrenceCount(request.occurrenceCount()));
        return record;
    }

    /**
     * 校验导入文件。
     *
     * @param file 上传文件
     */
    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "请选择CSV文件");
        }
        if (file.getSize() > MAX_IMPORT_FILE_SIZE) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "CSV文件不能超过2MB");
        }
    }

    /**
     * 解析 CSV 文件。
     *
     * @param file 上传文件
     * @return 题目请求列表
     */
    private List<SystemQuestionRequest> parseCsv(MultipartFile file) {
        List<SystemQuestionRequest> requests = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int rowIndex = 0;
            while ((line = reader.readLine()) != null) {
                rowIndex++;
                if (rowIndex == 1 && line.replace("\uFEFF", "").startsWith("code,")) {
                    continue;
                }
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                requests.add(parseCsvRow(line, rowIndex));
                if (requests.size() > MAX_IMPORT_ROWS) {
                    throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "单次最多导入1000道题");
                }
            }
        } catch (IOException exception) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "CSV文件读取失败");
        }
        if (requests.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "CSV文件没有可导入题目");
        }
        return requests;
    }

    /**
     * 解析单行 CSV。
     *
     * @param line CSV 行
     * @param rowIndex 行号
     * @return 题目请求
     */
    private SystemQuestionRequest parseCsvRow(String line, int rowIndex) {
        List<String> columns = splitCsvLine(line);
        if (columns.size() < 6) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "第" + rowIndex + "行字段数量不足");
        }
        return new SystemQuestionRequest(
                columns.get(0),
                columns.get(1),
                columns.get(2),
                columns.get(3),
                parseInteger(columns.get(4), DEFAULT_IMPORTANCE_SCORE, "第" + rowIndex + "行重要性评分不合法"),
                parseInteger(columns.get(5), DEFAULT_OCCURRENCE_COUNT, "第" + rowIndex + "行真实面试出现次数不合法")
        );
    }

    /**
     * 拆分 CSV 行，支持双引号转义。
     *
     * @param line CSV 行
     * @return 列数据
     */
    private List<String> splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                if (inQuote && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    inQuote = !inQuote;
                }
            } else if (ch == ',' && !inQuote) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    /**
     * 解析整数。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @param message 错误提示
     * @return 整数值
     */
    private int parseInteger(String value, int defaultValue, String message) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), message);
        }
    }

    /**
     * 生成唯一题目编码。
     *
     * @return 题目编码
     */
    private String generateQuestionCode() {
        for (int retry = 0; retry < 5; retry++) {
            String code = "SYS-" + System.currentTimeMillis() + "-" + RANDOM.nextInt(100_000, 999_999);
            if (systemQuestionAdminMapper.findByCodeAny(code) == null) {
                return code;
            }
        }
        throw new BusinessException(ResponseCode.SYSTEM_ERROR.code(), "题目编码生成失败，请重试");
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
     * 转换题目响应。
     *
     * @param record 题目记录
     * @return 响应对象
     */
    private SystemQuestionResponse toResponse(SystemQuestionRecord record) {
        return new SystemQuestionResponse(
                String.valueOf(record.getId()),
                record.getCode(),
                record.getQuestion(),
                record.getQuestionType(),
                record.getStandardAnswer(),
                record.getImportanceScore(),
                record.getOccurrenceCount(),
                toOffsetDateTime(record.getCreatedAt()),
                toOffsetDateTime(record.getUpdatedAt())
        );
    }

    /**
     * 转换本地时间。
     *
     * @param value 本地时间
     * @return 带偏移时间
     */
    private OffsetDateTime toOffsetDateTime(java.time.LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    /**
     * 校验必填文本。
     *
     * @param value 原始值
     * @param message 错误提示
     * @return 规整文本
     */
    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), message);
        }
        return value.trim();
    }

    /**
     * 规整题目编码。
     *
     * @param code 原始编码
     * @return 规整编码
     */
    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String safeCode = code.trim().toUpperCase(Locale.ROOT);
        if (safeCode.length() > 64) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "题目编码不能超过64个字符");
        }
        return safeCode;
    }

    /**
     * 规整题目分类。
     *
     * @param questionType 原始分类
     * @return 规整分类
     */
    private String normalizeQuestionType(String questionType) {
        String safeType = requireText(questionType, "题目分类不能为空").trim();
        if (safeType.length() > 32) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "题目分类不能超过32个字符");
        }
        return safeType;
    }

    /**
     * 规整重要性评分。
     *
     * @param value 原始评分
     * @return 安全评分
     */
    private int normalizeImportanceScore(Integer value) {
        int score = value == null ? DEFAULT_IMPORTANCE_SCORE : value;
        if (score < 0 || score > 100) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "重要性评分必须在0到100之间");
        }
        return score;
    }

    /**
     * 规整真实面试出现次数。
     *
     * @param value 原始次数
     * @return 安全次数
     */
    private int normalizeOccurrenceCount(Integer value) {
        int count = value == null ? DEFAULT_OCCURRENCE_COUNT : value;
        if (count < 0) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "真实面试出现次数不能小于0");
        }
        return count;
    }

    /**
     * 规整可选文本。
     *
     * @param value 原始文本
     * @return 规整文本
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
