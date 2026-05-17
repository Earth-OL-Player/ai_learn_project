package com.earth.online.player.ailearn.suggestion.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 提交建议请求。
 *
 * @param type 建议类型
 * @param content 建议内容
 */
public record CreateSuggestionRequest(
        @NotBlank(message = "建议类型不能为空")
        @Pattern(regexp = "FEATURE|EXPERIENCE|BUG|CONTENT", message = "建议类型不合法")
        String type,

        @NotBlank(message = "建议内容不能为空")
        @Size(min = 2, max = 1000, message = "建议内容长度需在2到1000位之间")
        String content
) {
}
