package com.agent.service;

import com.agent.dto.QueryResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a safe, bounded snapshot of an analysis result for persistence in analysis_tasks.result_json.
 *
 * Constraints (hard constraint 2):
 * - rows truncated to ≤ 200
 * - snapshot size capped at ~1 MB (larger → keep only summaries)
 * - sensitive SQL parameters redacted (only placeholder names kept, values replaced with ***)
 * - never stores prompt text, API key, connection info, or raw stack trace
 */
@Component
public class ResultSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(ResultSnapshotService.class);
    private static final int MAX_ROWS = 200;
    private static final int MAX_BYTES = 1_048_576; // 1 MB

    private final ObjectMapper objectMapper;

    public ResultSnapshotService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Serialize a bounded snapshot of the analysis result.
     *
     * @param taskId        for logging
     * @param intentJson    already-safe structured intent (or null)
     * @param sqlText       generated SQL (kept)
     * @param parameters    SQL named parameters — values redacted
     * @param validationPassed
     * @param violations    validation violations (kept, not sensitive)
     * @param queryResult   raw query result (rows truncated + values are DB data, kept bounded)
     * @param interpretationJson
     * @param chartJson
     */
    public String build(Long taskId, String intentJson, String sqlText,
                        Map<String, String> parameters, boolean validationPassed,
                        List<String> violations, QueryResult queryResult,
                        String interpretationJson, String chartJson) {
        try {
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("intent", parseSafe(intentJson));
            snap.put("sqlText", sqlText);
            snap.put("parameters", redactParameters(parameters));
            snap.put("validation", Map.of(
                    "passed", validationPassed,
                    "violations", violations == null ? List.of() : violations));
            snap.put("queryResult", toBoundedQueryResult(queryResult));
            snap.put("interpretation", parseSafe(interpretationJson));
            snap.put("chartSpec", parseSafe(chartJson));

            String json = objectMapper.writeValueAsString(snap);
            // Hard cap: if snapshot exceeds 1MB, drop to a minimal summary only
            // (queryResult keeps only columns/count/summary — no rows).
            if (json.getBytes().length > MAX_BYTES) {
                Map<String, Object> slim = new LinkedHashMap<>();
                slim.put("intent", parseSafe(intentJson));
                slim.put("sqlText", sqlText);
                slim.put("validation", Map.of("passed", validationPassed));
                slim.put("queryResult", slimQueryResult(queryResult));
                json = objectMapper.writeValueAsString(slim);
                log.warn("Result snapshot for task {} exceeded 1MB — reduced to summary", taskId);
            }
            return json;
        } catch (JsonProcessingException e) {
            log.error("Failed to build result snapshot for task {}", taskId, e);
            return "{\"error\":\"snapshot_failed\"}";
        }
    }

    /** Redact parameter values, keep keys only. */
    private Map<String, String> redactParameters(Map<String, String> params) {
        if (params == null) return Map.of();
        Map<String, String> red = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            red.put(e.getKey(), "***");
        }
        return red;
    }

    /** Ultra-slim query result for the 1MB fallback — no rows, just metadata. */
    private Map<String, Object> slimQueryResult(QueryResult q) {
        if (q == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("columns", q.columns());
        m.put("rowCount", q.rowCount());
        m.put("truncated", q.truncated());
        m.put("summary", q.summary());
        return m;
    }

    private QueryResult toBoundedQueryResult(QueryResult q) {
        if (q == null) return null;
        List<Map<String, Object>> bounded = q.rows();
        if (bounded.size() > MAX_ROWS) {
            bounded = bounded.subList(0, MAX_ROWS);
        }
        return new QueryResult(
                q.columns(), bounded, q.rowCount(), q.executionTimeMs(),
                null, q.truncated(), q.summary());
    }

    private Object parseSafe(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
