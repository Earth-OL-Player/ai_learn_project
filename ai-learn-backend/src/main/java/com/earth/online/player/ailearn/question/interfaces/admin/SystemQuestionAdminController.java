package com.earth.online.player.ailearn.question.interfaces.admin;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.util.CsvDownloadUtils;
import com.earth.online.player.ailearn.question.application.SystemQuestionAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 系统题库管理接口。
 */
@RestController
@RequestMapping("/api/v1/admin/system-questions")
public class SystemQuestionAdminController {

    private final SystemQuestionAdminService systemQuestionAdminService;

    /**
     * 创建系统题库管理接口。
     *
     * @param systemQuestionAdminService 系统题库管理服务
     */
    public SystemQuestionAdminController(SystemQuestionAdminService systemQuestionAdminService) {
        this.systemQuestionAdminService = systemQuestionAdminService;
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
    @GetMapping
    public ApiResponse<PageResponse<SystemQuestionResponse>> findPage(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String questionType) {
        return ApiResponse.success(systemQuestionAdminService.findPage(pageNo, pageSize, keyword, questionType));
    }

    /**
     * 查询题目详情。
     *
     * @param id 题目ID
     * @return 题目详情
     */
    @GetMapping("/{id}")
    public ApiResponse<SystemQuestionResponse> findDetail(@PathVariable Long id) {
        return ApiResponse.success(systemQuestionAdminService.findDetail(id));
    }

    /**
     * 查询题目分类。
     *
     * @return 分类列表
     */
    @GetMapping("/types")
    public ApiResponse<List<String>> findQuestionTypes() {
        return ApiResponse.success(systemQuestionAdminService.findQuestionTypes());
    }

    /**
     * 新增系统题目。
     *
     * @param request 保存请求
     * @return 保存后的题目
     */
    @PostMapping
    public ApiResponse<SystemQuestionResponse> create(@Valid @RequestBody SystemQuestionRequest request) {
        return ApiResponse.success(systemQuestionAdminService.create(request));
    }

    /**
     * 更新系统题目。
     *
     * @param id 题目ID
     * @param request 保存请求
     * @return 更新后的题目
     */
    @PutMapping("/{id}")
    public ApiResponse<SystemQuestionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SystemQuestionRequest request) {
        return ApiResponse.success(systemQuestionAdminService.update(id, request));
    }

    /**
     * 删除系统题目。
     *
     * @param id 题目ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success(systemQuestionAdminService.delete(id));
    }



    /**
     * 一键清空系统题库。
     *
     * @return 是否成功
     */
    @DeleteMapping("/clear")
    public ApiResponse<Boolean> clearAll() {
        return ApiResponse.success(systemQuestionAdminService.clearAll());
    }

    /**
     * 下载 CSV 导入模板。
     *
     * @return CSV 模板
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] content = systemQuestionAdminService.buildTemplate();
        return CsvDownloadUtils.buildUtf8CsvResponse("系统题库导入模板.csv", content);
    }

    /**
     * 预检 CSV 导入题目。
     *
     * @param file CSV 文件
     * @return 预检结果
     */
    @PostMapping(value = "/import/precheck", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportSystemQuestionsPrecheckResponse> precheckImportCsv(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(systemQuestionAdminService.precheckImportCsv(file));
    }

    /**
     * 上传 CSV 导入题目。
     *
     * @param file CSV 文件
     * @return 导入结果
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportSystemQuestionsResponse> importCsv(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(systemQuestionAdminService.importCsv(file));
    }
}
