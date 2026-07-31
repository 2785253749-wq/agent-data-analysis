package com.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Input for SQL generation.
 * Contains the recognized intent and the dataset ID for metadata lookup.
 */
public record SqlGenerationRequest(
        @NotBlank String question,
        @NotNull IntentDTO intent,
        @NotNull Long datasetId
) {}
