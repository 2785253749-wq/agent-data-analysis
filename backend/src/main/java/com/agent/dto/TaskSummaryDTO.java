package com.agent.dto;

import java.time.LocalDateTime;

/**
 * Lightweight task row for the history/trace list.
 * Deliberately excludes sqlText / intent / result — no sensitive fields on the list.
 */
public record TaskSummaryDTO(
        Long taskId,
        String question,
        Long datasetId,
        String datasetName,
        String status,
        Long durationMs,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {}
