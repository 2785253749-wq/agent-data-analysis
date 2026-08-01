package com.agent;

import com.agent.dto.*;
import com.agent.service.DatasetService;
import com.agent.service.QueryExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("QueryExecutionService")
class QueryExecutionServiceTest {

    @Autowired
    private QueryExecutionService executionService;

    @Autowired
    private DatasetService datasetService;

    private Long datasetId;

    @BeforeEach
    void setUp() {
        // Create test dataset with fields
        var ds = datasetService.create(new DatasetRequest(
                "查询测试", null, "query_test", 0L, true));
        datasetId = ds.id();
        datasetService.createField(datasetId, new DatasetFieldRequest(
                "id", "ID", "int", false, false, false, null));
        datasetService.createField(datasetId, new DatasetFieldRequest(
                "name", "名称", "varchar", true, false, true, null));
        datasetService.createField(datasetId, new DatasetFieldRequest(
                "amount", "金额", "decimal", false, true, true, null));
    }

    // ==================== Parameter Conversion ====================

    @Nested
    @DisplayName("convertNamedParams — parameter binding")
    class ParameterConversion {

        @Test
        @DisplayName("should convert ${param} to value")
        void shouldConvertDollarParams() {
            String result = executionService.convertNamedParams(
                    "SELECT * FROM t WHERE status = ${status}",
                    Map.of("status", "已完成"));
            assertTrue(result.contains("已完成"));
            assertFalse(result.contains("${status}"));
        }

        @Test
        @DisplayName("should convert :param to value")
        void shouldConvertColonParams() {
            String result = executionService.convertNamedParams(
                    "SELECT * FROM t WHERE status = :status",
                    Map.of("status", "active"));
            assertTrue(result.contains("'active'"), "String param should be quoted: " + result);
            assertFalse(result.contains(":status"));
        }

        @Test
        @DisplayName("should escape single quotes in param values")
        void shouldEscapeQuotes() {
            String result = executionService.convertNamedParams(
                    "SELECT * FROM t WHERE name = :name",
                    Map.of("name", "O'Brien"));
            assertTrue(result.contains("O''Brien"));
        }

        @Test
        @DisplayName("should handle null params map")
        void shouldHandleNullParams() {
            String sql = "SELECT * FROM t";
            String result = executionService.convertNamedParams(sql, null);
            assertEquals(sql, result);
        }

        @Test
        @DisplayName("should handle empty params map")
        void shouldHandleEmptyParams() {
            String sql = "SELECT * FROM t";
            String result = executionService.convertNamedParams(sql, Map.of());
            assertEquals(sql, result);
        }

        @Test
        @DisplayName("should quote string params in ${param} format")
        void shouldQuoteDollarStringParams() {
            String result = executionService.convertNamedParams(
                    "SELECT * FROM t WHERE status = ${status}",
                    Map.of("status", "completed"));
            assertTrue(result.contains("'completed'"), "String param must be quoted: " + result);
        }

        @Test
        @DisplayName("should not quote numeric params")
        void shouldNotQuoteNumericParams() {
            String result = executionService.convertNamedParams(
                    "SELECT * FROM t WHERE id = ${id}",
                    Map.of("id", "42"));
            assertTrue(result.contains("= 42"), "Numeric param must not be quoted: " + result);
        }

        @Test
        @DisplayName("should pass through true/false/null")
        void shouldPassThroughBooleans() {
            String result = executionService.convertNamedParams(
                    "SELECT * FROM t WHERE active = ${active}",
                    Map.of("active", "true"));
            assertTrue(result.contains("= true"));
        }
    }

    // ==================== Query Execution ====================

    @Nested
    @DisplayName("execute — query execution")
    class QueryExecution {

        @Test
        @DisplayName("should execute a clean SELECT and return QueryResult")
        void shouldExecuteCleanSelect() {
            // Use a system table that always exists in H2
            QueryResult result = executionService.execute(
                    "SELECT 1 AS num, 'hello' AS greeting", null);

            assertNotNull(result);
            assertFalse(result.columns().isEmpty());
            assertTrue(result.columns().contains("NUM") || result.columns().contains("num"));
            assertEquals(1, result.rowCount());
            assertTrue(result.executionTimeMs() >= 0);
            assertFalse(result.truncated());
        }

        @Test
        @DisplayName("should return empty QueryResult for query with no results")
        void shouldReturnEmptyForNoResults() {
            QueryResult result = executionService.execute(
                    "SELECT 1 AS x FROM DUAL WHERE 1 = 0", null);

            assertEquals(0, result.rowCount());
        }

        @Test
        @DisplayName("should measure execution time")
        void shouldMeasureExecutionTime() {
            QueryResult result = executionService.execute(
                    "SELECT 1 AS n", null);

            assertTrue(result.executionTimeMs() >= 0);
        }

        @Test
        @DisplayName("should include EXPLAIN plan")
        void shouldIncludeExplainPlan() {
            QueryResult result = executionService.execute(
                    "SELECT 1 AS n", null);

            // H2 supports EXPLAIN, so this should be non-null
            assertNotNull(result.explainPlan());
            assertFalse(result.explainPlan().isEmpty());
        }

        @Test
        @DisplayName("should set summary message")
        void shouldSetSummary() {
            QueryResult result = executionService.execute(
                    "SELECT 1 AS n", null);

            assertNotNull(result.summary());
            assertTrue(result.summary().contains("行"));
        }

        @Test
        @DisplayName("should reject execution of non-validated SQL via execute(validation, params)")
        void shouldRejectFailedValidation() {
            SqlValidationResult failed = SqlValidationResult.reject("bad", "test");

            assertThrows(IllegalArgumentException.class, () ->
                    executionService.execute(failed, Map.of()));
        }
    }
}
