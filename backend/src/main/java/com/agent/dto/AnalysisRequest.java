package com.agent.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalysisRequest(
        @NotBlank String question,
        Long datasetId,

        /** Optional — links this task to a conversation (multi-turn). Null keeps single-shot behavior. */
        Long conversationId
) {}
