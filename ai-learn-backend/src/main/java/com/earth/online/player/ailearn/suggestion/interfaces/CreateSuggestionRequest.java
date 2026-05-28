package com.earth.online.player.ailearn.suggestion.interfaces;

import com.earth.online.player.ailearn.interaction.domain.InteractionTextPolicy;
import com.earth.online.player.ailearn.suggestion.domain.SuggestionType;
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
        @Pattern(regexp = SuggestionType.TYPE_PATTERN, message = SuggestionType.INVALID_TYPE_MESSAGE)
        String type,

        @NotBlank(message = "建议内容不能为空")
        @Size(
                min = InteractionTextPolicy.MIN_CONTENT_LENGTH,
                max = InteractionTextPolicy.MAX_CONTENT_LENGTH,
                message = "建议内容长度需在" + InteractionTextPolicy.CONTENT_LENGTH_RANGE_TEXT + "之间"
        )
        String content
) {
}
