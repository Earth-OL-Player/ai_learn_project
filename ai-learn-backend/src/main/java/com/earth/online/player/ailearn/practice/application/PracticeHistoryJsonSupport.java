package com.earth.online.player.ailearn.practice.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.springframework.util.StringUtils;

/**
 * 刷题历史 JSON 读写辅助工具。
 */
final class PracticeHistoryJsonSupport {

    private static final String EMPTY_ARRAY_JSON = "[]";

    /**
     * 工具类不允许实例化。
     */
    private PracticeHistoryJsonSupport() {
    }

    /**
     * 读取历史列表，坏数据按空历史处理。
     *
     * @param objectMapper JSON 序列化器
     * @param historyJson 历史JSON
     * @param historyType 历史列表类型
     * @param <T> 历史消息类型
     * @return 历史消息列表
     */
    static <T> List<T> readList(ObjectMapper objectMapper, String historyJson, TypeReference<List<T>> historyType) {
        if (!StringUtils.hasText(historyJson)) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(historyJson, historyType);
        } catch (JsonProcessingException ignored) {
            // 历史解析失败不影响当前刷题主流程，直接从空历史恢复。
            return Collections.emptyList();
        }
    }

    /**
     * 序列化历史列表，失败时写入空数组。
     *
     * @param objectMapper JSON 序列化器
     * @param history 历史列表
     * @return JSON字符串
     */
    static String writeList(ObjectMapper objectMapper, List<?> history) {
        try {
            return objectMapper.writeValueAsString(history);
        } catch (JsonProcessingException ignored) {
            // 序列化失败时写入空数组，避免异常历史阻塞刷题流程。
            return EMPTY_ARRAY_JSON;
        }
    }
}
