package com.agent.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalysisRequest(
        @NotBlank String question,
        Long datasetId
) {}
