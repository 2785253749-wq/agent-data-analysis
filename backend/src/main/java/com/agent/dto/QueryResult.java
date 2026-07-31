package com.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Result of a safely executed SQL query.
 *
 * Contains the data rows, metadata, and execution metrics.
 * This is what gets passed to the interpretation (M5) and chart (M6) stages.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueryResult(
        /** Column names in order */
        List<String> columns,

        /** Data rows — each row is a map of column → value */
        List<Map<String, Object>> rows,

        /** Number of rows returned */
        int rowCount,

        /** Execution time in milliseconds */
        long executionTimeMs,

        /** EXPLAIN output (optional — for debugging/transparency) */
        String explainPlan,

        /** Whether the result was truncated (e.g., exceeded LIMIT) */
        boolean truncated,

        /** Human-readable summary of the execution */
        String summary
) {
    public static QueryResult empty() {
        return new QueryResult(List.of(), List.of(), 0, 0, null, false, "No results");
    }
}
