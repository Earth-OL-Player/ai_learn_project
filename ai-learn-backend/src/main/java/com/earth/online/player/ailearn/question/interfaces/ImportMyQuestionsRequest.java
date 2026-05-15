package com.earth.online.player.ailearn.question.interfaces;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 个人题库批量导入请求。
 *
 * @param mode 导入模式
 * @param questions 题目列表
 */
public record ImportMyQuestionsRequest(
        String mode,
        @NotEmpty(message = "导入题目不能为空") List<@Valid MyQuestionRequest> questions
) {
}
