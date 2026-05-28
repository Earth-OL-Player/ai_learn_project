package com.earth.online.player.ailearn.question.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.util.DateTimeUtils;
import com.earth.online.player.ailearn.common.util.IdRequestUtils;
import com.earth.online.player.ailearn.common.util.PageRequestUtils;
import com.earth.online.player.ailearn.common.util.TextUtils;
import com.earth.online.player.ailearn.question.domain.SystemQuestionLimits;
import com.earth.online.player.ailearn.question.infrastructure.SystemQuestionAdminMapper;
import com.earth.online.player.ailearn.question.infrastructure.SystemQuestionRecord;
import com.earth.online.player.ailearn.question.infrastructure.SystemQuestionWriteRecord;
import com.earth.online.player.ailearn.question.interfaces.admin.ImportSystemQuestionDiffResponse;
import com.earth.online.player.ailearn.question.interfaces.admin.ImportSystemQuestionIssueResponse;
import com.earth.online.player.ailearn.question.interfaces.admin.ImportSystemQuestionPreviewRowResponse;
import com.earth.online.player.ailearn.question.interfaces.admin.ImportSystemQuestionsResponse;
import com.earth.online.player.ailearn.question.interfaces.admin.ImportSystemQuestionsPrecheckResponse;
import com.earth.online.player.ailearn.question.interfaces.admin.SystemQuestionRequest;
import com.earth.online.player.ailearn.question.interfaces.admin.SystemQuestionResponse;
import com.earth.online.player.ailearn.user.application.CurrentUserService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 系统题库管理应用服务。
 */
@Service
public class SystemQuestionAdminService {

    private static final BigDecimal DEFAULT_IMPORTANCE_SCORE = BigDecimal.valueOf(60).setScale(1);
    private static final int DEFAULT_OCCURRENCE_COUNT = 0;
    private static final String CSV_HEADER = "code,question,question_type,standard_answer,importance_score,occurrence_count\n";
    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_CONFLICT = "CONFLICT";
    private static final String ACTION_ERROR = "ERROR";
    private static final String FIELD_FILE = "file";
    private static final String FIELD_CODE = "code";
    private static final String FIELD_QUESTION = "question";
    private static final String FIELD_QUESTION_TYPE = "questionType";
    private static final String FIELD_STANDARD_ANSWER = "standardAnswer";
    private static final String FIELD_IMPORTANCE_SCORE = "importanceScore";
    private static final String FIELD_OCCURRENCE_COUNT = "occurrenceCount";
    private static final String LABEL_FILE = "文件";
    private static final String LABEL_CODE = "题目编码";
    private static final String LABEL_QUESTION = "题目";
    private static final String LABEL_QUESTION_TYPE = "题目分类";
    private static final String LABEL_STANDARD_ANSWER = "参考答案";
    private static final String LABEL_IMPORTANCE_SCORE = "重要性评分";
    private static final String LABEL_OCCURRENCE_COUNT = "真实面试出现次数";
    private static final String AUTO_CODE_TEXT = "导入时自动生成";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CurrentUserService currentUserService;
    private final SystemQuestionAdminMapper systemQuestionAdminMapper;
    private final QuestionTypeCache questionTypeCache;

    /**
     * 创建系统题库管理服务。
     *
     * @param currentUserService 当前用户服务
     * @param systemQuestionAdminMapper 系统题库仓储
     * @param questionTypeCache 题目分类缓存
     */
    public SystemQuestionAdminService(
            CurrentUserService currentUserService,
            SystemQuestionAdminMapper systemQuestionAdminMapper,
            QuestionTypeCache questionTypeCache) {
        this.currentUserService = currentUserService;
        this.systemQuestionAdminMapper = systemQuestionAdminMapper;
        this.questionTypeCache = questionTypeCache;
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
        int safePageNo = PageRequestUtils.normalizePageNo(pageNo);
        int safePageSize = PageRequestUtils.normalizePageSize(pageSize);
        String safeKeyword = TextUtils.trimToNull(keyword);
        String safeQuestionType = TextUtils.trimToNull(questionType);
        int offset = PageRequestUtils.calculateOffset(safePageNo, safePageSize);

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
            questionTypeCache.invalidateAfterCommit();
            return toResponse(systemQuestionAdminMapper.findById(sameCode.getId()));
        }
        systemQuestionAdminMapper.insert(writeRecord);
        questionTypeCache.invalidateAfterCommit();
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
        questionTypeCache.invalidateAfterCommit();
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
        Long safeId = IdRequestUtils.requirePositive(id, "题目ID不合法");
        int affected = systemQuestionAdminMapper.deleteById(safeId);
        if (affected == 0) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "系统题目不存在或已删除");
        }
        questionTypeCache.invalidateAfterCommit();
        return true;
    }

    /**
     * 一键清空系统题库。
     *
     * @return 是否清空成功
     */
    public boolean clearAll() {
        requireSuperAdmin();
        systemQuestionAdminMapper.resetAllPracticeSessions();

        // 只清空题库主表，用户历史最高分汇总继续保留用于成长经验计算。
        systemQuestionAdminMapper.truncateQuestions();
        questionTypeCache.invalidateAfterCommit();
        return true;
    }

    /**
     * 查询系统题目分类。
     *
     * @return 分类列表
     */
    public List<String> findQuestionTypes() {
        requireSuperAdmin();
        return questionTypeCache.findQuestionTypes();
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
     * 预检系统题库 CSV。
     *
     * @param file CSV 文件
     * @return 预检结果
     */
    public ImportSystemQuestionsPrecheckResponse precheckImportCsv(MultipartFile file) {
        requireSuperAdmin();
        validateImportFile(file);
        return analyzeImportCsv(file).toPrecheckResponse();
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
        ImportAnalysis analysis = analyzeImportCsv(file);
        int createdCount = 0;
        int updatedCount = 0;

        // 只写入预检通过的行，字段错误和文件内冲突行直接跳过。
        for (ImportCandidate candidate : analysis.importableRows()) {
            SystemQuestionWriteRecord writeRecord = buildWriteRecord(candidate.request(), null);
            SystemQuestionRecord existing = StringUtils.hasText(writeRecord.getCode())
                    ? systemQuestionAdminMapper.findByCodeAny(writeRecord.getCode())
                    : null;
            if (existing == null) {
                systemQuestionAdminMapper.insert(writeRecord);
                createdCount++;
            } else {
                writeRecord.setId(existing.getId());
                systemQuestionAdminMapper.update(writeRecord);
                updatedCount++;
            }
        }
        ImportSystemQuestionsPrecheckResponse precheckResponse = analysis.toPrecheckResponse();
        int importedCount = createdCount + updatedCount;
        int skippedCount = precheckResponse.totalCount() - importedCount;
        if (importedCount > 0) {
            questionTypeCache.invalidateAfterCommit();
        }
        return new ImportSystemQuestionsResponse(
                importedCount,
                createdCount,
                updatedCount,
                skippedCount,
                precheckResponse.conflictCount(),
                precheckResponse.errorCount(),
                precheckResponse.rows(),
                precheckResponse.issues()
        );
    }

    /**
     * 校验当前用户必须是超级管理员。
     */
    private void requireSuperAdmin() {
        currentUserService.requireSuperAdmin("仅超级管理员可维护系统题库");
    }

    /**
     * 查询未删除题目。
     *
     * @param id 题目ID
     * @return 题目记录
     */
    private SystemQuestionRecord findExisting(Long id) {
        Long safeId = IdRequestUtils.requirePositive(id, "题目ID不合法");
        SystemQuestionRecord record = systemQuestionAdminMapper.findById(safeId);
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
        if (file.getSize() > SystemQuestionLimits.MAX_IMPORT_FILE_SIZE) {
            throw new BusinessException(
                    ResponseCode.PARAM_INVALID.code(),
                    SystemQuestionLimits.IMPORT_FILE_TOO_LARGE_MESSAGE
            );
        }
    }

    /**
     * 分析 CSV 导入计划。
     *
     * @param file 上传文件
     * @return 导入分析结果
     */
    private ImportAnalysis analyzeImportCsv(MultipartFile file) {
        CsvParseResult parseResult = parseCsvRows(file);
        List<ImportCandidate> importableRows = new ArrayList<>();
        List<ImportSystemQuestionPreviewRowResponse> previewRows = new ArrayList<>();
        List<ImportSystemQuestionIssueResponse> allIssues = new ArrayList<>(parseResult.issues());
        Set<String> seenCodes = new LinkedHashSet<>();

        // 逐行判断新增、更新、冲突和字段错误，预检阶段不写库。
        for (ParsedCsvRow row : parseResult.rows()) {
            RowAnalysis rowAnalysis = analyzeParsedRow(row, seenCodes);
            previewRows.add(rowAnalysis.previewRow());
            allIssues.addAll(rowAnalysis.previewRow().issues());
            if (rowAnalysis.candidate() != null) {
                importableRows.add(rowAnalysis.candidate());
            }
        }
        if (previewRows.isEmpty() && allIssues.isEmpty()) {
            allIssues.add(issue(0, FIELD_FILE, LABEL_FILE, "CSV文件没有可导入题目"));
        }
        return new ImportAnalysis(previewRows, allIssues, importableRows);
    }

    /**
     * 解析 CSV 文件为原始行。
     *
     * @param file 上传文件
     * @return CSV解析结果
     */
    private CsvParseResult parseCsvRows(MultipartFile file) {
        List<ParsedCsvRow> rows = new ArrayList<>();
        List<ImportSystemQuestionIssueResponse> issues = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            parseCsvRecords(reader, rows);
        } catch (IOException exception) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "CSV文件读取失败");
        }
        if (rows.size() > SystemQuestionLimits.MAX_IMPORT_ROWS) {
            throw new BusinessException(
                    ResponseCode.PARAM_INVALID.code(),
                    SystemQuestionLimits.IMPORT_ROWS_TOO_MANY_MESSAGE
            );
        }
        return new CsvParseResult(rows, issues);
    }

    /**
     * 从字符流中读取完整 CSV 记录。
     *
     * @param reader 字符读取器
     * @param rows CSV行集合
     * @throws IOException 文件读取异常
     */
    private void parseCsvRecords(BufferedReader reader, List<ParsedCsvRow> rows) throws IOException {
        String line;
        int physicalRowIndex = 0;
        int recordStartRowIndex = 0;
        StringBuilder recordBuilder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            physicalRowIndex++;
            if (recordBuilder.isEmpty() && !StringUtils.hasText(line)) {
                continue;
            }

            // CSV 字段内允许换行，必须累积到双引号闭合后再按一条记录解析。
            if (recordBuilder.isEmpty()) {
                recordStartRowIndex = physicalRowIndex;
            } else {
                recordBuilder.append('\n');
            }
            recordBuilder.append(line);
            appendCompleteCsvRecord(rows, recordBuilder, recordStartRowIndex);
        }
        if (!recordBuilder.isEmpty()) {
            rows.add(errorRow(recordStartRowIndex, "第" + recordStartRowIndex + "行CSV引号未闭合"));
        }
    }

    /**
     * 追加已读取完整的 CSV 记录。
     *
     * @param rows CSV行集合
     * @param recordBuilder 当前记录
     * @param recordStartRowIndex 起始行号
     */
    private void appendCompleteCsvRecord(List<ParsedCsvRow> rows, StringBuilder recordBuilder, int recordStartRowIndex) {
        if (!isCompleteCsvRecord(recordBuilder)) {
            return;
        }
        String record = recordBuilder.toString();
        recordBuilder.setLength(0);
        if (recordStartRowIndex == 1 && record.replace("\uFEFF", "").startsWith("code,")) {
            return;
        }
        rows.add(parseCsvRow(record, recordStartRowIndex));
    }

    /**
     * 判断 CSV 记录是否已经读取完整。
     *
     * @param recordBuilder CSV记录内容
     * @return 是否完整
     */
    private boolean isCompleteCsvRecord(StringBuilder recordBuilder) {
        boolean inQuote = false;
        for (int index = 0; index < recordBuilder.length(); index++) {
            char ch = recordBuilder.charAt(index);
            if (ch != '"') {
                continue;
            }
            if (inQuote && index + 1 < recordBuilder.length() && recordBuilder.charAt(index + 1) == '"') {
                index++;
                continue;
            }
            inQuote = !inQuote;
        }
        return !inQuote;
    }

    /**
     * 解析单行 CSV。
     *
     * @param line CSV 行
     * @param rowIndex 行号
     * @return 解析后的CSV行
     */
    private ParsedCsvRow parseCsvRow(String line, int rowIndex) {
        List<String> columns = splitCsvLine(line);
        List<ImportSystemQuestionIssueResponse> issues = new ArrayList<>();
        if (columns.size() != 6) {
            issues.add(issue(rowIndex, FIELD_FILE, LABEL_FILE, "第" + rowIndex + "行字段数量应为6列"));
            return rowWithIssues(rowIndex, columns, issues);
        }

        // 数字字段先按字符串解析，错误落到当前行而不是中断整批导入。
        BigDecimal importanceScore = parseDecimalForRow(columns.get(4), rowIndex, issues);
        Integer occurrenceCount = parseIntegerForRow(columns.get(5), rowIndex, issues);
        ParsedCsvRow row = new ParsedCsvRow(
                rowIndex,
                columns.get(0),
                columns.get(1),
                columns.get(2),
                columns.get(3),
                importanceScore,
                occurrenceCount,
                issues
        );
        validateParsedRow(row, issues);
        return row;
    }

    /**
     * 构造带字段错误的CSV行。
     *
     * @param rowIndex 行号
     * @param columns 原始列
     * @param issues 字段问题
     * @return 解析后的CSV行
     */
    private ParsedCsvRow rowWithIssues(
            int rowIndex,
            List<String> columns,
            List<ImportSystemQuestionIssueResponse> issues) {
        return new ParsedCsvRow(
                rowIndex,
                getColumn(columns, 0),
                getColumn(columns, 1),
                getColumn(columns, 2),
                getColumn(columns, 3),
                null,
                null,
                issues
        );
    }

    /**
     * 构造整行错误的CSV行。
     *
     * @param rowIndex 行号
     * @param message 错误说明
     * @return 解析后的CSV行
     */
    private ParsedCsvRow errorRow(int rowIndex, String message) {
        List<ImportSystemQuestionIssueResponse> issues = List.of(issue(rowIndex, FIELD_FILE, LABEL_FILE, message));
        return new ParsedCsvRow(rowIndex, "", "", "", "", null, null, issues);
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
     * 分析单个 CSV 题目行。
     *
     * @param row CSV行
     * @param seenCodes 已出现编码
     * @return 行分析结果
     */
    private RowAnalysis analyzeParsedRow(ParsedCsvRow row, Set<String> seenCodes) {
        List<ImportSystemQuestionIssueResponse> issues = new ArrayList<>(row.issues());
        if (!issues.isEmpty()) {
            return new RowAnalysis(buildPreviewRow(row, ACTION_ERROR, false, List.of(), issues), null);
        }
        SystemQuestionRequest request = toRequest(row);
        String code = TextUtils.trimToNull(request.code());
        if (StringUtils.hasText(code) && !seenCodes.add(code)) {
            issues.add(issue(row.rowIndex(), FIELD_CODE, LABEL_CODE, "题目编码在当前CSV中重复"));
            return new RowAnalysis(buildPreviewRow(row, ACTION_CONFLICT, false, List.of(), issues), null);
        }

        // 与数据库比对后生成新增或更新预览，管理员可先看影响范围。
        SystemQuestionRecord existing = StringUtils.hasText(code) ? systemQuestionAdminMapper.findByCodeAny(code) : null;
        String action = existing == null ? ACTION_CREATE : ACTION_UPDATE;
        List<ImportSystemQuestionDiffResponse> diffs = existing == null ? List.of() : buildDiffs(existing, request);
        ImportCandidate candidate = new ImportCandidate(request);
        return new RowAnalysis(buildPreviewRow(row, action, true, diffs, issues), candidate);
    }

    /**
     * 转换为保存请求。
     *
     * @param row CSV行
     * @return 保存请求
     */
    private SystemQuestionRequest toRequest(ParsedCsvRow row) {
        return new SystemQuestionRequest(
                normalizeCode(row.code()),
                requireText(row.question(), "题目不能为空"),
                normalizeQuestionType(row.questionType()),
                requireText(row.standardAnswer(), "参考答案不能为空"),
                normalizeImportanceScore(row.importanceScore()),
                normalizeOccurrenceCount(row.occurrenceCount())
        );
    }

    /**
     * 构建预览行。
     *
     * @param row CSV行
     * @param action 动作
     * @param importable 是否可导入
     * @param diffs 差异
     * @param issues 字段问题
     * @return 预览行
     */
    private ImportSystemQuestionPreviewRowResponse buildPreviewRow(
            ParsedCsvRow row,
            String action,
            boolean importable,
            List<ImportSystemQuestionDiffResponse> diffs,
            List<ImportSystemQuestionIssueResponse> issues) {
        return new ImportSystemQuestionPreviewRowResponse(
                row.rowIndex(),
                action,
                actionText(action),
                importable,
                StringUtils.hasText(row.code()) ? row.code().trim().toUpperCase(Locale.ROOT) : AUTO_CODE_TEXT,
                row.question(),
                row.questionType(),
                row.standardAnswer(),
                row.importanceScore(),
                row.occurrenceCount(),
                diffs,
                issues
        );
    }

    /**
     * 生成更新差异。
     *
     * @param existing 已存在题目
     * @param request CSV题目
     * @return 字段差异
     */
    private List<ImportSystemQuestionDiffResponse> buildDiffs(SystemQuestionRecord existing, SystemQuestionRequest request) {
        List<ImportSystemQuestionDiffResponse> diffs = new ArrayList<>();
        addDiff(diffs, FIELD_QUESTION, LABEL_QUESTION, existing.getQuestion(), request.question());
        addDiff(diffs, FIELD_QUESTION_TYPE, LABEL_QUESTION_TYPE, existing.getQuestionType(), request.questionType());
        addDiff(diffs, FIELD_STANDARD_ANSWER, LABEL_STANDARD_ANSWER, existing.getStandardAnswer(), request.standardAnswer());
        addDiff(diffs, FIELD_IMPORTANCE_SCORE, LABEL_IMPORTANCE_SCORE, existing.getImportanceScore(), request.importanceScore());
        addDiff(diffs, FIELD_OCCURRENCE_COUNT, LABEL_OCCURRENCE_COUNT, existing.getOccurrenceCount(), request.occurrenceCount());
        if (Boolean.TRUE.equals(existing.getDeleted())) {
            diffs.add(new ImportSystemQuestionDiffResponse("deleted", "删除状态", "已删除", "恢复可用"));
        }
        return diffs;
    }

    /**
     * 按需追加字段差异。
     *
     * @param diffs 差异集合
     * @param fieldName 字段名
     * @param fieldLabel 字段中文名
     * @param oldValue 旧值
     * @param newValue 新值
     */
    private void addDiff(
            List<ImportSystemQuestionDiffResponse> diffs,
            String fieldName,
            String fieldLabel,
            Object oldValue,
            Object newValue) {
        String oldText = valueToText(oldValue);
        String newText = valueToText(newValue);
        if (!oldText.equals(newText)) {
            diffs.add(new ImportSystemQuestionDiffResponse(fieldName, fieldLabel, oldText, newText));
        }
    }

    /**
     * 解析行内整数。
     *
     * @param value 原始值
     * @param rowIndex 行号
     * @param issues 字段问题
     * @return 整数值
     */
    private Integer parseIntegerForRow(String value, int rowIndex, List<ImportSystemQuestionIssueResponse> issues) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_OCCURRENCE_COUNT;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            issues.add(issue(rowIndex, FIELD_OCCURRENCE_COUNT, LABEL_OCCURRENCE_COUNT, "真实面试出现次数必须是整数"));
            return null;
        }
    }

    /**
     * 解析行内小数。
     *
     * @param value 原始值
     * @param rowIndex 行号
     * @param issues 字段问题
     * @return 小数值
     */
    private BigDecimal parseDecimalForRow(String value, int rowIndex, List<ImportSystemQuestionIssueResponse> issues) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_IMPORTANCE_SCORE;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            issues.add(issue(rowIndex, FIELD_IMPORTANCE_SCORE, LABEL_IMPORTANCE_SCORE, "重要性评分必须是数字"));
            return null;
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
                DateTimeUtils.toOffsetDateTime(record.getCreatedAt()),
                DateTimeUtils.toOffsetDateTime(record.getUpdatedAt())
        );
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
        if (safeCode.length() > SystemQuestionLimits.MAX_CODE_LENGTH) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), SystemQuestionLimits.CODE_TOO_LONG_MESSAGE);
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
        if (safeType.length() > SystemQuestionLimits.MAX_QUESTION_TYPE_LENGTH) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), SystemQuestionLimits.QUESTION_TYPE_TOO_LONG_MESSAGE);
        }
        return safeType;
    }

    /**
     * 规整重要性评分。
     *
     * @param value 原始评分
     * @return 安全评分
     */
    private BigDecimal normalizeImportanceScore(BigDecimal value) {
        BigDecimal score = value == null ? DEFAULT_IMPORTANCE_SCORE : value.setScale(1, RoundingMode.HALF_UP);
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.valueOf(100)) > 0) {
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
     * 校验解析后的CSV行。
     *
     * @param row CSV行
     * @param issues 字段问题
     */
    private void validateParsedRow(ParsedCsvRow row, List<ImportSystemQuestionIssueResponse> issues) {
        validateText(
                row.rowIndex(),
                FIELD_QUESTION,
                LABEL_QUESTION,
                row.question(),
                SystemQuestionLimits.MAX_LONG_TEXT_LENGTH,
                true,
                issues
        );
        validateText(
                row.rowIndex(),
                FIELD_QUESTION_TYPE,
                LABEL_QUESTION_TYPE,
                row.questionType(),
                SystemQuestionLimits.MAX_QUESTION_TYPE_LENGTH,
                true,
                issues
        );
        validateText(
                row.rowIndex(),
                FIELD_STANDARD_ANSWER,
                LABEL_STANDARD_ANSWER,
                row.standardAnswer(),
                SystemQuestionLimits.MAX_LONG_TEXT_LENGTH,
                true,
                issues
        );
        validateText(
                row.rowIndex(),
                FIELD_CODE,
                LABEL_CODE,
                row.code(),
                SystemQuestionLimits.MAX_CODE_LENGTH,
                false,
                issues
        );
        validateScore(row.rowIndex(), row.importanceScore(), issues);
        validateOccurrenceCount(row.rowIndex(), row.occurrenceCount(), issues);
    }

    /**
     * 校验文本字段。
     *
     * @param rowIndex 行号
     * @param fieldName 字段名
     * @param fieldLabel 字段中文名
     * @param value 字段值
     * @param maxLength 最大长度
     * @param required 是否必填
     * @param issues 字段问题
     */
    private void validateText(
            int rowIndex,
            String fieldName,
            String fieldLabel,
            String value,
            int maxLength,
            boolean required,
            List<ImportSystemQuestionIssueResponse> issues) {
        if (required && !StringUtils.hasText(value)) {
            issues.add(issue(rowIndex, fieldName, fieldLabel, fieldLabel + "不能为空"));
            return;
        }
        if (StringUtils.hasText(value) && value.trim().length() > maxLength) {
            issues.add(issue(rowIndex, fieldName, fieldLabel, fieldLabel + "不能超过" + maxLength + "个字符"));
        }
    }

    /**
     * 校验重要性评分。
     *
     * @param rowIndex 行号
     * @param value 评分
     * @param issues 字段问题
     */
    private void validateScore(int rowIndex, BigDecimal value, List<ImportSystemQuestionIssueResponse> issues) {
        if (value == null) {
            return;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            issues.add(issue(rowIndex, FIELD_IMPORTANCE_SCORE, LABEL_IMPORTANCE_SCORE, "重要性评分必须在0到100之间"));
        }
    }

    /**
     * 校验真实面试出现次数。
     *
     * @param rowIndex 行号
     * @param value 次数
     * @param issues 字段问题
     */
    private void validateOccurrenceCount(int rowIndex, Integer value, List<ImportSystemQuestionIssueResponse> issues) {
        if (value != null && value < 0) {
            issues.add(issue(rowIndex, FIELD_OCCURRENCE_COUNT, LABEL_OCCURRENCE_COUNT, "真实面试出现次数不能小于0"));
        }
    }

    /**
     * 获取指定列。
     *
     * @param columns 列集合
     * @param index 下标
     * @return 列值
     */
    private String getColumn(List<String> columns, int index) {
        return index < columns.size() ? columns.get(index) : "";
    }

    /**
     * 构造字段问题。
     *
     * @param rowIndex 行号
     * @param fieldName 字段名
     * @param fieldLabel 字段中文名
     * @param message 问题说明
     * @return 字段问题
     */
    private ImportSystemQuestionIssueResponse issue(int rowIndex, String fieldName, String fieldLabel, String message) {
        return new ImportSystemQuestionIssueResponse(rowIndex, fieldName, fieldLabel, message);
    }

    /**
     * 转换动作中文文案。
     *
     * @param action 动作
     * @return 中文文案
     */
    private String actionText(String action) {
        return switch (action) {
            case ACTION_CREATE -> "新增";
            case ACTION_UPDATE -> "更新";
            case ACTION_CONFLICT -> "冲突";
            default -> "错误";
        };
    }

    /**
     * 转换预览展示文本。
     *
     * @param value 原始值
     * @return 展示文本
     */
    private String valueToText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    /**
     * CSV解析结果。
     *
     * @param rows CSV行
     * @param issues 文件级问题
     */
    private record CsvParseResult(List<ParsedCsvRow> rows, List<ImportSystemQuestionIssueResponse> issues) {
    }

    /**
     * 解析后的CSV行。
     *
     * @param rowIndex 行号
     * @param code 题目编码
     * @param question 题目
     * @param questionType 题目分类
     * @param standardAnswer 参考答案
     * @param importanceScore 重要性评分
     * @param occurrenceCount 真实面试出现次数
     * @param issues 字段问题
     */
    private record ParsedCsvRow(
            int rowIndex,
            String code,
            String question,
            String questionType,
            String standardAnswer,
            BigDecimal importanceScore,
            Integer occurrenceCount,
            List<ImportSystemQuestionIssueResponse> issues) {
    }

    /**
     * 单行分析结果。
     *
     * @param previewRow 预览行
     * @param candidate 可导入候选
     */
    private record RowAnalysis(ImportSystemQuestionPreviewRowResponse previewRow, ImportCandidate candidate) {
    }

    /**
     * 可导入候选行。
     *
     * @param request 保存请求
     */
    private record ImportCandidate(SystemQuestionRequest request) {
    }

    /**
     * 导入分析结果。
     *
     * @param rows 预览行
     * @param issues 字段问题
     * @param importableRows 可导入行
     */
    private record ImportAnalysis(
            List<ImportSystemQuestionPreviewRowResponse> rows,
            List<ImportSystemQuestionIssueResponse> issues,
            List<ImportCandidate> importableRows) {

        /**
         * 转换为预检响应。
         *
         * @return 预检响应
         */
        private ImportSystemQuestionsPrecheckResponse toPrecheckResponse() {
            int createdCount = countAction(ACTION_CREATE);
            int updatedCount = countAction(ACTION_UPDATE);
            int conflictCount = countAction(ACTION_CONFLICT);
            int errorCount = countAction(ACTION_ERROR) + Math.max(0, issues.size() - rowIssueCount());
            return new ImportSystemQuestionsPrecheckResponse(
                    rows.size(),
                    createdCount + updatedCount,
                    createdCount,
                    updatedCount,
                    conflictCount,
                    errorCount,
                    rows,
                    issues
            );
        }

        /**
         * 统计指定动作数量。
         *
         * @param action 动作
         * @return 数量
         */
        private int countAction(String action) {
            return (int) rows.stream().filter(row -> action.equals(row.action())).count();
        }

        /**
         * 统计行内问题数量。
         *
         * @return 行内问题数量
         */
        private int rowIssueCount() {
            return rows.stream().mapToInt(row -> row.issues().size()).sum();
        }
    }
}
