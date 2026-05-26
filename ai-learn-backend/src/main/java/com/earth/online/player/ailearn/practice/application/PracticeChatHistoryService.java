package com.earth.online.player.ailearn.practice.application;

import com.earth.online.player.ailearn.practice.domain.PracticeConstants;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeMapper;
import com.earth.online.player.ailearn.practice.infrastructure.PracticeSessionRecord;
import com.earth.online.player.ailearn.practice.interfaces.PracticeChatMessageResponse;
import com.earth.online.player.ailearn.practice.interfaces.PracticeMessageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 当前轮刷题跨端展示聊天记录服务。
 */
@Service
public class PracticeChatHistoryService {

    private static final String USER_ROLE = "user";
    private static final String ASSISTANT_ROLE = "assistant";
    private static final TypeReference<List<PracticeChatMessageResponse>> CHAT_HISTORY_TYPE = new TypeReference<>() {
    };

    private final PracticeMapper practiceMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建当前轮刷题跨端展示聊天记录服务。
     *
     * @param practiceMapper 刷题仓储
     * @param objectMapper JSON 序列化器
     */
    public PracticeChatHistoryService(PracticeMapper practiceMapper, ObjectMapper objectMapper) {
        this.practiceMapper = practiceMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 读取当前轮跨端展示聊天记录。
     *
     * @param historyJson 聊天记录JSON
     * @return 聊天消息列表
     */
    public List<PracticeChatMessageResponse> readChatHistory(String historyJson) {
        if (!StringUtils.hasText(historyJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(historyJson, CHAT_HISTORY_TYPE);
        } catch (JsonProcessingException ignored) {
            // 展示历史解析失败不影响当前刷题状态恢复，直接返回空列表。
            return Collections.emptyList();
        }
    }

    /**
     * 用助手消息覆盖当前轮聊天记录。
     *
     * @param userId 用户ID
     * @param assistantResponse 助手响应
     */
    public void replaceWithAssistantMessage(Long userId, PracticeMessageResponse assistantResponse) {
        practiceMapper.updateChatHistory(userId, writeChatHistory(List.of(toAssistantMessage(assistantResponse))));
    }

    /**
     * 追加一轮用户消息和助手响应。
     *
     * @param userId 用户ID
     * @param userContent 用户消息
     * @param assistantResponse 助手响应
     */
    public void appendConversationTurn(Long userId, String userContent, PracticeMessageResponse assistantResponse) {
        PracticeSessionRecord session = practiceMapper.findSession(userId);
        List<PracticeChatMessageResponse> messages = new ArrayList<>(readChatHistory(session == null ? null : session.getChatHistoryJson()));
        messages.add(toUserMessage(userContent));
        messages.add(toAssistantMessage(assistantResponse));

        // 当前轮记录只保留有限消息数，切题时会被覆盖，避免无限增长。
        int fromIndex = Math.max(0, messages.size() - PracticeConstants.MAX_CHAT_HISTORY_MESSAGES);
        practiceMapper.updateChatHistory(userId, writeChatHistory(messages.subList(fromIndex, messages.size())));
    }

    /**
     * 构造用户消息。
     *
     * @param content 用户输入
     * @return 聊天消息
     */
    private PracticeChatMessageResponse toUserMessage(String content) {
        return new PracticeChatMessageResponse(
                USER_ROLE,
                limitText(content, PracticeConstants.MAX_CHAT_HISTORY_CONTENT_LENGTH),
                null,
                null
        );
    }

    /**
     * 构造助手消息。
     *
     * @param response 助手响应
     * @return 聊天消息
     */
    private PracticeChatMessageResponse toAssistantMessage(PracticeMessageResponse response) {
        return new PracticeChatMessageResponse(
                ASSISTANT_ROLE,
                limitText(response.message(), PracticeConstants.MAX_CHAT_HISTORY_CONTENT_LENGTH),
                PracticeConstants.ACTION_QUESTION.equals(response.action()) ? response.question() : null,
                response.grading()
        );
    }

    /**
     * 序列化聊天记录。
     *
     * @param messages 聊天消息
     * @return JSON字符串
     */
    private String writeChatHistory(List<PracticeChatMessageResponse> messages) {
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (JsonProcessingException ignored) {
            // 序列化失败时写入空数组，避免坏数据阻塞主流程。
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

        // 截断后保留明确提示，便于跨端展示时知道内容被压缩过。
        return text.substring(0, maxLength) + "……";
    }
}
