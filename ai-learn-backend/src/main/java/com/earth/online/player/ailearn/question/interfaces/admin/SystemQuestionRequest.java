package com.earth.online.player.ailearn.question.interfaces.admin;

import com.earth.online.player.ailearn.question.domain.SystemQuestionLimits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 系统题目保存请求。
 *
 * @param code 题目编码
 * @param question 题目内容
 * @param questionType 题目分类
 * @param standardAnswer 参考答案
 * @param importanceScore 重要性评分
 * @param occurrenceCount 真实面试出现次数
 */
public record SystemQuestionRequest(
        @Size(
                max = SystemQuestionLimits.MAX_CODE_LENGTH,
                message = SystemQuestionLimits.CODE_TOO_LONG_MESSAGE
        )
        String code,

        @NotBlank(message = "题目不能为空")
        @Size(
                max = SystemQuestionLimits.MAX_LONG_TEXT_LENGTH,
                message = SystemQuestionLimits.QUESTION_TOO_LONG_MESSAGE
        )
        String question,

        @NotBlank(message = "题目分类不能为空")
        @Size(
                max = SystemQuestionLimits.MAX_QUESTION_TYPE_LENGTH,
                message = SystemQuestionLimits.QUESTION_TYPE_TOO_LONG_MESSAGE
        )
        String questionType,

        @NotBlank(message = "参考答案不能为空")
        @Size(
                max = SystemQuestionLimits.MAX_LONG_TEXT_LENGTH,
                message = SystemQuestionLimits.STANDARD_ANSWER_TOO_LONG_MESSAGE
        )
        String standardAnswer,

        @DecimalMin(value = "0.0", message = "重要性评分不能小于0")
        @DecimalMax(value = "100.0", message = "重要性评分不能大于100") BigDecimal importanceScore,

        @Min(value = 0, message = "真实面试出现次数不能小于0") Integer occurrenceCount
) {
}
