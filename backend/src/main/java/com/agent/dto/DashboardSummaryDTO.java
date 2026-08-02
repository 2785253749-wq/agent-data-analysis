package com.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Read-only dashboard summary — all aggregation happens in the DB.
 * successRate = COMPLETED / (COMPLETED + FAILED + CANCELLED); null when no terminal tasks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardSummaryDTO(
        long datasetCount,
        long analysisCount,
        Double successRate,
        List<TrendPoint> last7DaysTrend,
        List<RecentTaskDTO> recentTasks,
        List<FailureCount> commonFailures
) {
    public record TrendPoint(String date, long count) {}
    public record RecentTaskDTO(
            Long taskId, String question, String status, String datasetName,
            java.time.LocalDateTime createdAt, Long durationMs) {}
    public record FailureCount(String reason, long count) {}
}
