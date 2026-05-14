package com.earth.online.player.ailearn.suggestion.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 提交建议请求。
 *
 * @param title 建议标题
 * @param type 建议类型
 * @param content 建议内容
 */
public record CreateSuggestionRequest(
        @NotBlank(message = "建议标题不能为空")
        @Size(min = 2, max = 80, message = "建议标题长度需在2到80位之间")
        String title,

        @NotBlank(message = "建议类型不能为空")
        @Pattern(regexp = "FEATURE|EXPERIENCE|CONTENT|BUG|OTHER", message = "建议类型不合法")
        String type,

        @NotBlank(message = "建议内容不能为空")
        @Size(min = 5, max = 2000, message = "建议内容长度需在5到2000位之间")
        String content
) {
}
