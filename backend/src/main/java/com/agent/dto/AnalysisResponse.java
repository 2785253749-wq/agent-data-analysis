package com.agent.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AnalysisResponse(
        Long taskId,
        String question,
        String status,
        IntentDTO intent,
        SqlResultDTO sqlResult,
        SqlValidationResult validationResult,
        QueryResult queryResult,
        InterpretationDTO interpretation,
        ChartSpecDTO chartSpec,
        String errorMessage,
        List<StepInfo> steps,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public record StepInfo(
            String stepType,
            String status,
            Long durationMs
    ) {}
}
