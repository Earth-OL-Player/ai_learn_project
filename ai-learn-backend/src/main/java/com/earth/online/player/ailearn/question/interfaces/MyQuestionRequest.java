package com.earth.online.player.ailearn.question.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 个人题目保存请求。
 *
 * @param title 题目标题
 * @param content 题目内容
 * @param questionType 题型
 * @param difficulty 难度
 * @param tags 标签
 * @param knowledgePoints 知识点名称
 * @param standardAnswer 参考答案
 * @param analysis 解析
 */
public record MyQuestionRequest(
        @NotBlank(message = "题目标题不能为空") @Size(max = 120, message = "题目标题不能超过120个字符") String title,
        @NotBlank(message = "题目内容不能为空") @Size(max = 5000, message = "题目内容不能超过5000个字符") String content,
        @NotBlank(message = "题型不能为空") String questionType,
        @NotBlank(message = "难度不能为空") String difficulty,
        List<String> tags,
        List<String> knowledgePoints,
        @NotBlank(message = "参考答案不能为空") @Size(max = 5000, message = "参考答案不能超过5000个字符") String standardAnswer,
        @Size(max = 5000, message = "解析不能超过5000个字符") String analysis
) {
}
