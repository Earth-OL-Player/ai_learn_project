package com.earth.online.player.ailearn.interaction.application;

import com.earth.online.player.ailearn.common.exception.BusinessException;
import com.earth.online.player.ailearn.common.response.ResponseCode;
import com.earth.online.player.ailearn.interaction.domain.InteractionTextPolicy;

/**
 * 互动内容应用层校验器。
 */
public final class InteractionContentValidator {

    private static final String UNSUPPORTED_CONTENT_MESSAGE = "仅支持纯文字，不能使用表情和艾特";

    /**
     * 工具类不允许实例化。
     */
    private InteractionContentValidator() {
    }

    /**
     * 校验互动纯文字内容。
     *
     * @param content 已规整内容
     * @param contentName 内容业务名称
     */
    public static void validatePlainTextContent(String content, String contentName) {
        if (InteractionTextPolicy.hasInvalidLength(content)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), buildLengthMessage(contentName));
        }
        if (InteractionTextPolicy.containsUnsupportedContent(content)) {
            throw new BusinessException(ResponseCode.PARAM_INVALID.code(), UNSUPPORTED_CONTENT_MESSAGE);
        }
    }

    /**
     * 组装内容长度错误文案。
     *
     * @param contentName 内容业务名称
     * @return 内容长度错误文案
     */
    private static String buildLengthMessage(String contentName) {
        return contentName + "内容长度需在" + InteractionTextPolicy.CONTENT_LENGTH_RANGE_TEXT + "之间";
    }
}
