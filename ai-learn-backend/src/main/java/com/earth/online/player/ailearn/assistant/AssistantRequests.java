package com.earth.online.player.ailearn.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 助手公共接口只接收受限内容和操作版本，不接收身份或模型配置。 */
public final class AssistantRequests {
    private AssistantRequests() { }

    public record Message(
            @NotBlank @Size(max = 4000) String content,
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9-]{1,64}") String clientRequestId,
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9-]{1,64}") String clientInstanceId,
            JsonNode pageContext) { }

    public record Control(@NotBlank @Size(max = 64) String clientInstanceId, int executionEpoch) { }

    public record Action(@NotBlank String operationId, @NotBlank String clientInstanceId,
                         int executionEpoch, int payloadVersion, @NotBlank String payloadHash,
                         int stepIndex, @NotBlank String status, @Size(max = 200) String reason) { }

    public record Confirmation(@NotBlank String operationId, @NotBlank String clientInstanceId,
                               int executionEpoch, int payloadVersion, @NotBlank String payloadHash,
                               boolean confirmed) { }

    public record Draft(@NotBlank String operationId, @NotBlank String clientInstanceId,
                        int executionEpoch, int payloadVersion, @NotBlank @Size(max = 1000) String content) { }
}
