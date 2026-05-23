package com.earth.online.player.ailearn.practice.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.practice.domain.PracticeConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 刷题消息意图识别服务。
 */
@Service
public class PracticeMessageClassifier {

    /**
     * 规整消息内容。
     *
     * @param content 原始内容
     * @return 安全内容
     */
    public String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), "请输入内容");
        }

        // 统一去除首尾空白，后续意图判断只处理稳定文本。
        return content.trim();
    }

    /**
     * 判断是否为出题请求。
     *
     * @param content 用户输入
     * @return 是否出题
     */
    public boolean isQuestionRequest(String content) {
        return content.contains("出题")
                || content.contains("来一题")
                || content.contains("来一道")
                || content.contains("随机")
                || content.contains("开始")
                || content.contains("刷题")
                || content.contains("题目")
                || content.contains("类型的题");
    }

    /**
     * 判断是否为明确的新出题请求。
     *
     * @param content 用户输入
     * @return 是否明确请求新题
     */
    public boolean isExplicitQuestionRequest(String content) {
        return content.contains("出题")
                || content.contains("来一题")
                || content.contains("来一道")
                || content.contains("随机")
                || content.contains("开始刷题")
                || content.contains("类型的题");
    }

    /**
     * 判断是否为下一题请求。
     *
     * @param content 用户输入
     * @return 是否下一题
     */
    public boolean isNextRequest(String content) {
        return content.contains("下一题") || content.contains("换一题") || content.contains("再来一题");
    }

    /**
     * 判断是否为重新回答请求。
     *
     * @param content 用户输入
     * @return 是否重新回答
     */
    public boolean isRetryRequest(String content) {
        return content.contains("重新回答") || content.contains("再答一次") || content.contains("重答");
    }

    /**
     * 判断用户输入是否偏离刷题上下文。
     *
     * @param content 用户输入
     * @return 是否无关
     */
    public boolean isUnrelatedToPractice(String content) {
        // 当前迭代取消大模型相关性判断，避免额外模型调用拉长用户等待时间。
        return PracticeConstants.UNRELATED_WORDS.stream().anyMatch(content::contains);
    }
}
