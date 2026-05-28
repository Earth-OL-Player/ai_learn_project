package com.earth.online.player.ailearn.practice.application;

import com.earth.online.player.ailearn.common.util.TextUtils;
import com.earth.online.player.ailearn.practice.domain.PracticeConstants;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 当前题讨论历史服务。
 */
@Service
public class DiscussionHistoryService {

    private static final TypeReference<List<DiscussionHistoryMessage>> DISCUSSION_HISTORY_TYPE = new TypeReference<>() {
    };

    private final PracticeMapper practiceMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建当前题讨论历史服务。
     *
     * @param practiceMapper 刷题仓储
     * @param objectMapper JSON 序列化器
     */
    public DiscussionHistoryService(PracticeMapper practiceMapper, ObjectMapper objectMapper) {
        this.practiceMapper = practiceMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存当前题讨论历史。
     *
     * @param userId 用户ID
     * @param historyJson 原始历史JSON
     * @param userMessage 用户问题
     * @param assistantReply AI回复
     */
    public void saveDiscussionHistory(Long userId, String historyJson, String userMessage, String assistantReply) {
        List<DiscussionHistoryMessage> history = new ArrayList<>(
                PracticeHistoryJsonSupport.readList(objectMapper, historyJson, DISCUSSION_HISTORY_TYPE)
        );
        history.add(new DiscussionHistoryMessage(
                "user",
                TextUtils.limitText(userMessage, PracticeConstants.MAX_DISCUSSION_HISTORY_CONTENT_LENGTH)
        ));
        history.add(new DiscussionHistoryMessage(
                "assistant",
                TextUtils.limitText(assistantReply, PracticeConstants.MAX_DISCUSSION_HISTORY_CONTENT_LENGTH)
        ));

        // 只保留最近若干条消息，避免上下文过长影响响应速度。
        int fromIndex = Math.max(0, history.size() - PracticeConstants.MAX_DISCUSSION_HISTORY_MESSAGES);
        List<DiscussionHistoryMessage> limitedHistory = history.subList(fromIndex, history.size());
        practiceMapper.updateDiscussionHistory(userId, PracticeHistoryJsonSupport.writeList(objectMapper, limitedHistory));
    }

    /**
     * 当前题讨论历史消息。
     *
     * @param role 消息角色
     * @param content 消息内容
     */
    private record DiscussionHistoryMessage(String role, String content) {
    }
}
