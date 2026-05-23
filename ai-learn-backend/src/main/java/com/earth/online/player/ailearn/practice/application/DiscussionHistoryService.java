package com.earth.online.player.ailearn.practice.application;

import com.earth.online.player.ailearn.practice.domain.PracticeConstants;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
        List<DiscussionHistoryMessage> history = new ArrayList<>(readDiscussionHistory(historyJson));
        history.add(new DiscussionHistoryMessage("user", limitText(userMessage, PracticeConstants.MAX_DISCUSSION_HISTORY_CONTENT_LENGTH)));
        history.add(new DiscussionHistoryMessage("assistant", limitText(assistantReply, PracticeConstants.MAX_DISCUSSION_HISTORY_CONTENT_LENGTH)));

        // 只保留最近若干条消息，避免上下文过长影响响应速度。
        int fromIndex = Math.max(0, history.size() - PracticeConstants.MAX_DISCUSSION_HISTORY_MESSAGES);
        List<DiscussionHistoryMessage> limitedHistory = history.subList(fromIndex, history.size());
        practiceMapper.updateDiscussionHistory(userId, writeDiscussionHistory(limitedHistory));
    }

    /**
     * 读取当前题讨论历史。
     *
     * @param historyJson 历史JSON
     * @return 历史消息
     */
    private List<DiscussionHistoryMessage> readDiscussionHistory(String historyJson) {
        if (!StringUtils.hasText(historyJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(historyJson, DISCUSSION_HISTORY_TYPE);
        } catch (JsonProcessingException ignored) {
            // 历史解析失败不影响当前追问，直接从空历史重新开始。
            return Collections.emptyList();
        }
    }

    /**
     * 序列化当前题讨论历史。
     *
     * @param history 历史消息
     * @return JSON字符串
     */
    private String writeDiscussionHistory(List<DiscussionHistoryMessage> history) {
        try {
            return objectMapper.writeValueAsString(history);
        } catch (JsonProcessingException ignored) {
            // 序列化失败时写入空数组，避免脏历史阻塞主流程。
            return "[]";
        }
    }

    /**
     * 限制文本长度。
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private String limitText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        // 截断后保留明确提示，便于模型理解上下文被压缩过。
        return text.substring(0, maxLength) + "……";
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
