package com.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Contract for DeepSeek SQL generation output.
 *
 * The model returns structured SQL JSON that must pass through
 * SqlSafetyService (M3) before execution.
 *
 * Per spec section 6.2: only SELECT/WITH SELECT statements allowed.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SqlResultDTO(

        /** The generated SQL query (SELECT or WITH...SELECT only) */
        String sql,

        /** Named parameters for prepared statement binding (optional) */
        Map<String, String> parameters,

        /** Tables referenced in the SQL — for permission validation */
        List<String> usedTables,

        /** Fields referenced in the SQL — for whitelist validation */
        List<String> usedFields,

        /** Human-readable explanation of what this SQL does */
        String explanation

) {
    /**
     * Quick check: does this look like a read-only query?
     * Simple heuristic — full validation is in SqlSafetyService (M3).
     */
    public boolean looksLikeSelect() {
        if (sql == null || sql.isBlank()) return false;
        String trimmed = sql.trim().toUpperCase();
        return trimmed.startsWith("SELECT") || trimmed.startsWith("WITH");
    }
}
