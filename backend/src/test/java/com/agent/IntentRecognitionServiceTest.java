package com.agent;

import com.agent.dto.IntentDTO;
import com.agent.dto.IntentRequest;
import com.agent.dto.DatasetRequest;
import com.agent.service.DatasetService;
import com.agent.service.IntentRecognitionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("IntentRecognitionService")
class IntentRecognitionServiceTest {

    @Autowired
    private IntentRecognitionService intentService;

    @Autowired
    private DatasetService datasetService;

    // ==================== JSON Parsing ====================

    @Nested
    @DisplayName("parseIntent — JSON parsing")
    class ParseIntent {

        @Test
        @DisplayName("should parse valid aggregation intent JSON")
        void shouldParseAggregationIntent() {
            String json = """
                    {
                      "intentType": "aggregation",
                      "metrics": ["订单金额", "订单数量"],
                      "dimensions": ["地区", "月份"],
                      "filters": [{"field": "订单状态", "operator": "=", "value": "已完成"}],
                      "timeRange": {"type": "latest_n_days", "start": "30", "end": null},
                      "comparison": null,
                      "needsClarification": false,
                      "clarificationQuestions": []
                    }""";

            IntentDTO intent = intentService.parseIntent(json);

            assertEquals("aggregation", intent.intentType());
            assertEquals(2, intent.metrics().size());
            assertEquals("订单金额", intent.metrics().get(0));
            assertEquals(2, intent.dimensions().size());
            assertEquals(1, intent.filters().size());
            assertEquals("订单状态", intent.filters().get(0).field());
            assertNotNull(intent.timeRange());
            assertEquals("latest_n_days", intent.timeRange().type());
            assertFalse(intent.needsClarification());
        }

        @Test
        @DisplayName("should parse markdown-wrapped JSON")
        void shouldParseMarkdownWrappedJson() {
            String json = """
                    ```json
                    {
                      "intentType": "query",
                      "metrics": [],
                      "dimensions": [],
                      "filters": [],
                      "timeRange": null,
                      "comparison": null,
                      "needsClarification": false,
                      "clarificationQuestions": []
                    }
                    ```""";

            IntentDTO intent = intentService.parseIntent(json);

            assertEquals("query", intent.intentType());
            assertFalse(intent.needsClarification());
        }

        @Test
        @DisplayName("should parse ranking intent type")
        void shouldParseRankingIntent() {
            String json = """
                    {
                      "intentType": "ranking",
                      "metrics": ["销售额"],
                      "dimensions": ["商品名称"],
                      "filters": [],
                      "timeRange": {"type": "latest_n_months", "start": "3", "end": null},
                      "comparison": null,
                      "needsClarification": false,
                      "clarificationQuestions": []
                    }""";

            IntentDTO intent = intentService.parseIntent(json);

            assertEquals("ranking", intent.intentType());
            assertEquals("销售额", intent.metrics().get(0));
            assertEquals("latest_n_months", intent.timeRange().type());
        }

        @Test
        @DisplayName("should parse comparison intent with comparison field")
        void shouldParseComparisonIntent() {
            String json = """
                    {
                      "intentType": "comparison",
                      "metrics": ["订单金额"],
                      "dimensions": ["月份"],
                      "filters": [],
                      "timeRange": {"type": "latest_n_months", "start": "12", "end": null},
                      "comparison": "同比增长",
                      "needsClarification": false,
                      "clarificationQuestions": []
                    }""";

            IntentDTO intent = intentService.parseIntent(json);

            assertEquals("comparison", intent.intentType());
            assertEquals("同比增长", intent.comparison());
        }

        @Test
        @DisplayName("should handle needsClarification=true with questions")
        void shouldHandleNeedsClarification() {
            String json = """
                    {
                      "intentType": "query",
                      "metrics": [],
                      "dimensions": [],
                      "filters": [],
                      "timeRange": null,
                      "comparison": null,
                      "needsClarification": true,
                      "clarificationQuestions": ["请指定要查询的指标", "请指定时间范围"]
                    }""";

            IntentDTO intent = intentService.parseIntent(json);

            assertTrue(intent.needsClarification());
            assertEquals(2, intent.clarificationQuestions().size());
        }

        @Test
        @DisplayName("should throw on invalid JSON")
        void shouldThrowOnInvalidJson() {
            String json = "not json at all";

            assertThrows(IllegalArgumentException.class, () ->
                    intentService.parseIntent(json));
        }

        @Test
        @DisplayName("should throw on empty string")
        void shouldThrowOnEmptyString() {
            assertThrows(IllegalArgumentException.class, () ->
                    intentService.parseIntent(""));
        }

        @Test
        @DisplayName("should parse intent with between filter")
        void shouldParseBetweenFilter() {
            String json = """
                    {
                      "intentType": "query",
                      "metrics": ["订单金额"],
                      "dimensions": [],
                      "filters": [{"field": "订单金额", "operator": "between", "value": "100", "value2": "1000"}],
                      "timeRange": null,
                      "comparison": null,
                      "needsClarification": false,
                      "clarificationQuestions": []
                    }""";

            IntentDTO intent = intentService.parseIntent(json);

            assertEquals(1, intent.filters().size());
            assertEquals("between", intent.filters().get(0).operator());
            assertEquals("100", intent.filters().get(0).value());
            assertEquals("1000", intent.filters().get(0).value2());
        }
    }

    // ==================== Context Building (with real dataset) ====================

    @Nested
    @DisplayName("recognize with dataset context")
    class RecognizeWithContext {

        @Test
        @DisplayName("should accept IntentRequest with datasetId")
        void shouldAcceptIntentRequestWithDatasetId() throws Exception {
            // Create a test dataset with fields and metrics
            DatasetRequest dsReq = new DatasetRequest(
                    "意图测试数据集", "测试用", "intent_test", 0L, true);
            var ds = datasetService.create(dsReq);

            // Build an intent request
            IntentRequest request = new IntentRequest(
                    "今年每个月的销售额是多少？", ds.id());

            assertNotNull(request.question());
            assertEquals(ds.id(), request.datasetId());
        }
    }
}
