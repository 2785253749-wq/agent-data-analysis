package com.agent;

import com.agent.dto.*;
import com.agent.service.DatasetService;
import com.agent.service.SqlGenerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SqlGenerationService")
class SqlGenerationServiceTest {

    @Autowired
    private SqlGenerationService sqlService;

    @Autowired
    private DatasetService datasetService;

    // ==================== JSON Parsing ====================

    @Nested
    @DisplayName("parseSqlResult — JSON parsing")
    class ParseSqlResult {

        @Test
        @DisplayName("should parse valid SELECT SQL JSON")
        void shouldParseValidSelectSql() {
            String json = """
                    {
                      "sql": "SELECT region, SUM(amount) as total FROM sales WHERE status = ${status} GROUP BY region LIMIT 20",
                      "parameters": {"status": "已完成"},
                      "usedTables": ["sales"],
                      "usedFields": ["region", "amount", "status"],
                      "explanation": "按地区汇总销售额"
                    }""";

            SqlResultDTO result = sqlService.parseSqlResult(json);

            assertNotNull(result.sql());
            assertTrue(result.looksLikeSelect());
            assertEquals(1, result.usedTables().size());
            assertEquals("sales", result.usedTables().get(0));
            assertEquals(3, result.usedFields().size());
            assertNotNull(result.parameters());
            assertEquals("已完成", result.parameters().get("status"));
            assertEquals("按地区汇总销售额", result.explanation());
        }

        @Test
        @DisplayName("should parse WITH...SELECT SQL")
        void shouldParseWithSelectSql() {
            String json = """
                    {
                      "sql": "WITH monthly AS (SELECT month, SUM(amount) as m FROM sales GROUP BY month) SELECT * FROM monthly LIMIT 10",
                      "parameters": {},
                      "usedTables": ["sales"],
                      "usedFields": ["month", "amount"],
                      "explanation": "使用CTE先按月汇总再查询"
                    }""";

            SqlResultDTO result = sqlService.parseSqlResult(json);

            assertTrue(result.looksLikeSelect());
            assertNotNull(result.sql());
        }

        @Test
        @DisplayName("should parse markdown-wrapped JSON")
        void shouldParseMarkdownWrappedJson() {
            String json = """
                    ```json
                    {
                      "sql": "SELECT * FROM products LIMIT 50",
                      "parameters": {},
                      "usedTables": ["products"],
                      "usedFields": ["*"],
                      "explanation": "查询产品列表"
                    }
                    ```""";

            SqlResultDTO result = sqlService.parseSqlResult(json);

            assertEquals("SELECT * FROM products LIMIT 50", result.sql());
            assertTrue(result.looksLikeSelect());
        }

        @Test
        @DisplayName("should detect non-SELECT SQL via looksLikeSelect")
        void shouldDetectNonSelectSql() {
            SqlResultDTO deleteResult = new SqlResultDTO(
                    "DELETE FROM users WHERE id = 1", null, List.of(), List.of(), "bad");
            assertFalse(deleteResult.looksLikeSelect());

            SqlResultDTO insertResult = new SqlResultDTO(
                    "INSERT INTO t VALUES (1)", null, List.of(), List.of(), "bad");
            assertFalse(insertResult.looksLikeSelect());

            SqlResultDTO selectResult = new SqlResultDTO(
                    "SELECT * FROM t", null, List.of(), List.of(), "good");
            assertTrue(selectResult.looksLikeSelect());
        }

        @Test
        @DisplayName("should reject empty SQL in looksLikeSelect")
        void shouldRejectEmptySql() {
            SqlResultDTO empty = new SqlResultDTO("", null, List.of(), List.of(), "");
            assertFalse(empty.looksLikeSelect());

            SqlResultDTO nullResult = new SqlResultDTO(null, null, List.of(), List.of(), "");
            assertFalse(nullResult.looksLikeSelect());
        }

        @Test
        @DisplayName("should throw on invalid JSON")
        void shouldThrowOnInvalidJson() {
            assertThrows(IllegalArgumentException.class, () ->
                    sqlService.parseSqlResult("not json"));
        }

        @Test
        @DisplayName("should throw on empty string")
        void shouldThrowOnEmptyString() {
            assertThrows(IllegalArgumentException.class, () ->
                    sqlService.parseSqlResult(""));
        }

        @Test
        @DisplayName("should parse SQL with multiple filters and ORDER BY")
        void shouldParseComplexSql() {
            String json = """
                    {
                      "sql": "SELECT o.region, o.product, SUM(o.amount) as revenue FROM orders o WHERE o.status = ${status} AND o.amount > ${min_amount} AND o.created_at >= ${start} GROUP BY o.region, o.product ORDER BY revenue DESC LIMIT 100",
                      "parameters": {"status": "已完成", "min_amount": "100", "start": "2026-01-01"},
                      "usedTables": ["orders"],
                      "usedFields": ["region", "product", "amount", "status", "created_at"],
                      "explanation": "按地区和产品汇总已完成订单金额"
                    }""";

            SqlResultDTO result = sqlService.parseSqlResult(json);

            assertEquals(3, result.parameters().size());
            assertEquals(5, result.usedFields().size());
            assertTrue(result.sql().contains("LIMIT 100"));
        }

        @Test
        @DisplayName("should parse SQL with HAVING clause")
        void shouldParseSqlWithHaving() {
            String json = """
                    {
                      "sql": "SELECT region, SUM(amount) as total FROM sales GROUP BY region HAVING SUM(amount) > ${min_total} ORDER BY total DESC LIMIT 10",
                      "parameters": {"min_total": "10000"},
                      "usedTables": ["sales"],
                      "usedFields": ["region", "amount"],
                      "explanation": "汇总各地区销售额，只显示总额超过一万的地区"
                    }""";

            SqlResultDTO result = sqlService.parseSqlResult(json);

            assertTrue(result.sql().contains("HAVING"));
            assertTrue(result.sql().contains("LIMIT 10"));
        }
    }

    // ==================== Integration ====================

    @Nested
    @DisplayName("generate with dataset context")
    class GenerateWithContext {

        @Test
        @DisplayName("should accept SqlGenerationRequest with intent and dataset")
        void shouldAcceptSqlGenerationRequest() throws Exception {
            // Create a dataset with fields
            var ds = datasetService.create(new DatasetRequest(
                    "SQL测试集", null, "sql_test", 0L, true));

            datasetService.createField(ds.id(), new DatasetFieldRequest(
                    "amount", "金额", "decimal", false, true, true, null));
            datasetService.createField(ds.id(), new DatasetFieldRequest(
                    "region", "地区", "varchar", true, false, true, null));

            IntentDTO intent = new IntentDTO(
                    "aggregation",
                    List.of("总金额"), List.of("地区"), List.of(),
                    null, null, false, List.of());

            SqlGenerationRequest request = new SqlGenerationRequest(
                    "各地区销售额汇总", intent, ds.id());

            assertNotNull(request.question());
            assertEquals(ds.id(), request.datasetId());
            assertEquals("aggregation", request.intent().intentType());
        }
    }
}
