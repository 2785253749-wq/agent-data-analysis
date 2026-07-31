package com.agent.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Input for intent recognition.
 */
public record IntentRequest(
        @NotBlank(message = "问题不能为空")
        String question,

        /** Dataset ID — used to load field/metric metadata from the database */
        Long datasetId
) {}
