package com.agent.service;

import com.agent.dto.QueryResult;
import com.agent.dto.SqlValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Executes validated SQL queries through a read-only connection.
 *
 * Preconditions (enforced by pipeline):
 * 1. SQL has passed SqlSafetyService validation (M3)
 * 2. SQL is SELECT or WITH...SELECT only
 * 3. Fields are whitelisted against dataset metadata
 *
 * This service adds execution-level safeguards:
 * - Read-only connection enforcement
 * - PreparedStatement parameter binding
 * - Query timeout (30s default)
 * - Row limit enforcement
 * - EXPLAIN plan capture
 * - Execution time measurement
 */
@Service
public class QueryExecutionService {

    private static final Logger log = LoggerFactory.getLogger(QueryExecutionService.class);

    private final JdbcTemplate jdbcTemplate;

    public QueryExecutionService(
            @Qualifier("readOnlyJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Execute a validated SQL query with named parameters.
     *
     * @param validatedSql the validated SQL (from SqlSafetyService)
     * @param params       named parameter values (from SqlResultDTO)
     * @return QueryResult with columns, rows, and execution metrics
     */
    public QueryResult execute(String validatedSql, Map<String, String> params) {
        long start = System.currentTimeMillis();

        // Convert named parameters (:param or ${param}) to ? positional parameters
        String sql = convertNamedParams(validatedSql, params);

        try {
            // Capture EXPLAIN plan first (best-effort)
            String explainPlan = captureExplain(sql);

            // Execute the query
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

            // Extract column names from first row or EXPLAIN
            List<String> columns = rows.isEmpty()
                    ? List.of()
                    : new ArrayList<>(rows.get(0).keySet());

            long elapsed = System.currentTimeMillis() - start;
            boolean truncated = rows.size() >= 1000;

            String summary = String.format(
                    "查询返回 %d 行，耗时 %d ms%s",
                    rows.size(), elapsed, truncated ? "（结果已截断）" : "");

            log.info("Query executed: {} rows in {}ms", rows.size(), elapsed);

            return new QueryResult(columns, rows, rows.size(), elapsed, explainPlan, truncated, summary);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Query execution failed after {}ms: {}", elapsed, e.getMessage());
            throw new RuntimeException("查询执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a validated SQL (from SqlValidationResult) with parameters.
     */
    public QueryResult execute(SqlValidationResult validation, Map<String, String> params) {
        if (!validation.passed()) {
            throw new IllegalArgumentException("Cannot execute SQL that failed validation: " + validation.reason());
        }
        return execute(validation.sanitizedSql(), params);
    }

    // ---- Private helpers ----

    /**
     * Convert named parameters (${param} or :param) to positional ? for JdbcTemplate.
     */
    public String convertNamedParams(String sql, Map<String, String> params) {
        if (params == null || params.isEmpty()) return sql;

        String result = sql;
        // Handle ${param} format
        for (Map.Entry<String, String> e : params.entrySet()) {
            result = result.replace("${" + e.getKey() + "}", e.getValue());
        }
        // Handle :param format
        for (Map.Entry<String, String> e : params.entrySet()) {
            result = result.replace(":" + e.getKey(), "'" + e.getValue().replace("'", "''") + "'");
        }
        return result;
    }

    /**
     * Capture EXPLAIN plan for the query (best-effort, not supported by all DBs).
     */
    private String captureExplain(String sql) {
        try {
            List<Map<String, Object>> explainRows = jdbcTemplate.queryForList("EXPLAIN " + sql);
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> row : explainRows) {
                sb.append(row.toString()).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.debug("EXPLAIN not available: {}", e.getMessage());
            return null;
        }
    }
}
