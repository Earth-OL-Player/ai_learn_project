package com.earth.online.player.ailearn.model.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.response.PageResponse;
import com.earth.online.player.ailearn.common.util.CsvDownloadUtils;
import com.earth.online.player.ailearn.model.application.ModelEntitlementService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员兑换码接口。
 */
@RestController
@RequestMapping("/api/v1/admin/redemption-codes")
public class AdminRedemptionCodeController {

    private final ModelEntitlementService modelEntitlementService;

    /**
     * 创建管理员兑换码接口。
     *
     * @param modelEntitlementService 模型权益服务
     */
    public AdminRedemptionCodeController(ModelEntitlementService modelEntitlementService) {
        this.modelEntitlementService = modelEntitlementService;
    }

    /**
     * 分页查询兑换码。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param keyword 关键词
     * @param codeType 兑换码类型
     * @param status 状态
     * @return 兑换码分页
     */
    @GetMapping
    public ApiResponse<PageResponse<AdminRedemptionCodeResponse>> findPage(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String codeType,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(modelEntitlementService.findRedemptionPage(pageNo, pageSize, keyword, codeType, status));
    }

    /**
     * 批量生成兑换码。
     *
     * @param request 生成请求
     * @return 新生成兑换码
     */
    @PostMapping("/generate")
    public ApiResponse<List<AdminRedemptionCodeResponse>> generate(@RequestBody AdminRedemptionCodeGenerateRequest request) {
        return ApiResponse.success(modelEntitlementService.generateRedemptionCodes(request));
    }

    /**
     * 编辑未使用兑换码。
     *
     * @param id 兑换码ID
     * @param request 编辑请求
     * @return 编辑后兑换码
     */
    @PutMapping("/{id}")
    public ApiResponse<AdminRedemptionCodeResponse> update(
            @PathVariable Long id,
            @RequestBody AdminRedemptionCodeUpdateRequest request) {
        return ApiResponse.success(modelEntitlementService.updateRedemptionCode(id, request));
    }

    /**
     * 删除未使用兑换码。
     *
     * @param id 兑换码ID
     * @return 是否删除成功
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success(modelEntitlementService.deleteRedemptionCode(id));
    }

    /**
     * 导出兑换码 CSV。
     *
     * @param keyword 关键词
     * @param codeType 兑换码类型
     * @param status 状态
     * @return CSV 文件
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String codeType,
            @RequestParam(required = false) String status) {
        byte[] content = modelEntitlementService.exportRedemptionCodes(keyword, codeType, status);
        return CsvDownloadUtils.buildUtf8CsvResponse("模型权益兑换码.csv", content);
    }
}
