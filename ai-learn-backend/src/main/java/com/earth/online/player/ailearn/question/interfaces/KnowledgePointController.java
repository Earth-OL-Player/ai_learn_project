package com.earth.online.player.ailearn.question.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.question.application.KnowledgePointService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识点接口控制器。
 */
@RestController
@RequestMapping("/api/v1/knowledge-points")
public class KnowledgePointController {

    private final KnowledgePointService knowledgePointService;

    /**
     * 创建知识点控制器。
     *
     * @param knowledgePointService 知识点服务
     */
    public KnowledgePointController(KnowledgePointService knowledgePointService) {
        this.knowledgePointService = knowledgePointService;
    }

    /**
     * 查询知识点列表。
     *
     * @return 知识点列表
     */
    @GetMapping
    public ApiResponse<List<KnowledgePointResponse>> findAll() {
        return ApiResponse.success(knowledgePointService.findAll());
    }
}
