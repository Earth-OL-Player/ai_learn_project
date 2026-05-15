package com.earth.online.player.ailearn.rag.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.common.security.AuthContext;
import com.earth.online.player.ailearn.common.security.AuthenticatedUser;
import com.earth.online.player.ailearn.rag.infrastructure.RagIndexTaskMapper;
import com.earth.online.player.ailearn.rag.infrastructure.RagIndexTaskRecord;
import com.earth.online.player.ailearn.rag.interfaces.RagIndexTaskRequest;
import com.earth.online.player.ailearn.rag.interfaces.RagIndexTaskResponse;
import com.earth.online.player.ailearn.rag.interfaces.RagSearchRequest;
import com.earth.online.player.ailearn.rag.interfaces.RagSearchSnippetResponse;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * RAG任务应用服务。
 */
@Service
public class RagService {

    private static final String STATUS_SUCCESS = "SUCCESS";

    private final RagIndexTaskMapper ragIndexTaskMapper;

    /**
     * 创建 RAG 服务。
     *
     * @param ragIndexTaskMapper RAG任务仓储
     */
    public RagService(RagIndexTaskMapper ragIndexTaskMapper) {
        this.ragIndexTaskMapper = ragIndexTaskMapper;
    }

    /**
     * 提交入库任务摘要。
     *
     * @param request 任务请求
     * @return 任务响应
     */
    @Transactional
    public RagIndexTaskResponse submitIndexTask(RagIndexTaskRequest request) {
        Long userId = currentUserId();
        String taskId = UUID.randomUUID().toString();
        String sourceType = StringUtils.hasText(request == null ? null : request.sourceType())
                ? request.sourceType().trim().toUpperCase(java.util.Locale.ROOT)
                : "QUESTION";
        String message = "已记录本地入库任务摘要；向量入库由 ai-service 的 /internal/v1/rag/index-tasks 执行。";
        ragIndexTaskMapper.insertTask(taskId, userId, sourceType, STATUS_SUCCESS, message);
        RagIndexTaskRecord record = ragIndexTaskMapper.findMine(taskId, userId);
        return toResponse(record);
    }

    /**
     * 查询我的入库任务。
     *
     * @param taskId 任务ID
     * @return 任务响应
     */
    public RagIndexTaskResponse getTask(String taskId) {
        RagIndexTaskRecord record = ragIndexTaskMapper.findMine(taskId, currentUserId());
        if (record == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND.code(), "RAG入库任务不存在");
        }
        return toResponse(record);
    }

    /**
     * 后端检索占位接口，真实向量检索由 ai-service 提供。
     *
     * @param request 检索请求
     * @return 检索片段
     */
    public List<RagSearchSnippetResponse> search(RagSearchRequest request) {
        if (request == null || !StringUtils.hasText(request.query())) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "检索文本不能为空");
        }
        return Collections.emptyList();
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
     * 转换任务响应。
     *
     * @param record 任务记录
     * @return 响应
     */
    private RagIndexTaskResponse toResponse(RagIndexTaskRecord record) {
        return new RagIndexTaskResponse(
                record.getTaskId(),
                record.getSourceType(),
                record.getStatus(),
                record.getMessage(),
                record.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                record.getUpdatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
        );
    }
}
