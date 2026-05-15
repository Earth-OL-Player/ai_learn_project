package com.earth.online.player.ailearn.answer.interfaces;

import com.earth.online.player.ailearn.answer.application.AnswerRecordService;
import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.common.response.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 答题记录接口。
 */
@RestController
@RequestMapping("/api/v1/answer-records")
public class AnswerRecordController {

    private final AnswerRecordService answerRecordService;

    /**
     * 创建答题记录接口。
     *
     * @param answerRecordService 答题记录应用服务
     */
    public AnswerRecordController(AnswerRecordService answerRecordService) {
        this.answerRecordService = answerRecordService;
    }

    /**
     * 查询我的答题记录。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @return 分页答题记录
     */
    @GetMapping("/me")
    public ApiResponse<PageResponse<AnswerRecordResponse>> findMine(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(answerRecordService.findCurrentUserPage(pageNo, pageSize));
    }
}
