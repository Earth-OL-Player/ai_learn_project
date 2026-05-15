package com.earth.online.player.ailearn.rag.interfaces;

import com.earth.online.player.ailearn.common.response.ApiResponse;
import com.earth.online.player.ailearn.rag.application.RagService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG管理接口。
 */
@RestController
@RequestMapping("/api/v1/rag")
public class RagController {

    private final RagService ragService;

    /**
     * 创建 RAG 接口。
     *
     * @param ragService RAG 服务
     */
    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /** 提交入库任务。 */
    @PostMapping("/index-tasks")
    public ApiResponse<RagIndexTaskResponse> submitIndexTask(@RequestBody(required = false) RagIndexTaskRequest request) {
        return ApiResponse.success(ragService.submitIndexTask(request));
    }

    /** 查询入库任务。 */
    @GetMapping("/index-tasks/{taskId}")
    public ApiResponse<RagIndexTaskResponse> getTask(@PathVariable String taskId) {
        return ApiResponse.success(ragService.getTask(taskId));
    }

    /** 检索知识片段。 */
    @PostMapping("/search")
    public ApiResponse<List<RagSearchSnippetResponse>> search(@RequestBody RagSearchRequest request) {
        return ApiResponse.success(ragService.search(request));
    }
}
