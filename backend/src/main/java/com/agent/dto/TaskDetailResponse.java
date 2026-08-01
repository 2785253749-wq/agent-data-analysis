package com.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Task detail — built from the persisted, bounded snapshot (rows ≤ 200, params redacted).
 * sqlText is only returned to the task owner / admin (enforced in service).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaskDetailResponse(
        Long taskId,
        String question,
        Long datasetId,
        String datasetName,
        String status,
        Long durationMs,
        LocalDateTime createdAt,
        LocalDateTime completedAt,

        /** Parsed from snapshot.result_json — never contains prompt text / keys. */
        Object intent,
        String sqlText,
        Map<String, String> parameters,
        Object validation,
        Object queryResult,
        Object interpretation,
        Object chartSpec,

        /** Sanitized failure reason (whitelist-first + regex fallback). */
        String errorMessage,

        List<TaskStepDTO> steps
) {}
