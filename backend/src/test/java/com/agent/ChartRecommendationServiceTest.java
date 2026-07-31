package com.agent;

import com.agent.dto.*;
import com.agent.service.ChartRecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ChartRecommendationService")
class ChartRecommendationServiceTest {

    @Autowired
    private ChartRecommendationService chartService;

    private QueryResult makeResult(String... columnsAndValues) {
        // columnsAndValues: "col1", "val1", "col2", "val2", ...
        List<String> cols = new java.util.ArrayList<>();
        java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
        for (int i = 0; i < columnsAndValues.length; i += 2) {
            String col = columnsAndValues[i];
            cols.add(col);
            try {
                row.put(col, Double.parseDouble(columnsAndValues[i + 1]));
            } catch (NumberFormatException e) {
                row.put(col, columnsAndValues[i + 1]);
            }
        }
        return new QueryResult(cols, List.of(row), 1, 10, null, false, "test");
    }

    @Nested
    @DisplayName("determineType — chart type recommendation")
    class DetermineType {

        @Test
        @DisplayName("should recommend line chart for aggregation with time dimension")
        void shouldRecommendLineForTimeAggregation() {
            var intent = new IntentDTO("aggregation", List.of("销售额"), List.of("月份"),
                    List.of(), null, null, false, List.of());
            var result = makeResult("月份", "2026-01", "销售额", "1000");

            assertEquals("line", chartService.determineType(intent, result));
        }

        @Test
        @DisplayName("should recommend pie chart for single dimension aggregation")
        void shouldRecommendPie() {
            var intent = new IntentDTO("aggregation", List.of("销售额"), List.of("地区"),
                    List.of(), null, null, false, List.of());
            var result = makeResult("地区", "华东", "销售额", "500");

            assertEquals("pie", chartService.determineType(intent, result));
        }

        @Test
        @DisplayName("should recommend bar chart for multi-dimension aggregation")
        void shouldRecommendBar() {
            var intent = new IntentDTO("aggregation", List.of("销售额"), List.of("地区", "产品"),
                    List.of(), null, null, false, List.of());
            var result = makeResult("地区", "华东", "产品", "A", "销售额", "500");

            // Multi-dimension → bar (not pie)
            assertEquals("bar", chartService.determineType(intent, result));
        }

        @Test
        @DisplayName("should recommend horizontal_bar for ranking")
        void shouldRecommendHorizontalBarForRanking() {
            var intent = new IntentDTO("ranking", List.of("销售额"), List.of("商品"),
                    List.of(), null, null, false, List.of());
            var result = makeResult("商品", "A", "销售额", "1000");

            assertEquals("horizontal_bar", chartService.determineType(intent, result));
        }

        @Test
        @DisplayName("should recommend table for detail intent")
        void shouldRecommendTableForDetail() {
            var intent = new IntentDTO("detail", List.of(), List.of(),
                    List.of(), null, null, false, List.of());
            var result = makeResult("订单ID", "001", "金额", "100");

            assertEquals("table", chartService.determineType(intent, result));
        }

        @Test
        @DisplayName("should recommend table for query intent")
        void shouldRecommendTableForQuery() {
            var intent = new IntentDTO("query", List.of(), List.of(),
                    List.of(), null, null, false, List.of());
            var result = makeResult("名称", "test");

            assertEquals("table", chartService.determineType(intent, result));
        }

        @Test
        @DisplayName("should recommend bar for comparison intent")
        void shouldRecommendBarForComparison() {
            var intent = new IntentDTO("comparison", List.of("销售额"), List.of("年份"),
                    List.of(), null, "同比增长", false, List.of());
            var result = makeResult("年份", "2025", "销售额", "1000");

            assertEquals("bar", chartService.determineType(intent, result));
        }

        @Test
        @DisplayName("should recommend scatter for correlation intent with 2+ metrics")
        void shouldRecommendScatterForCorrelation() {
            var intent = new IntentDTO("correlation", List.of("销售额", "利润"), List.of(),
                    List.of(), null, null, false, List.of());
            var result = makeResult("销售额", "1000", "利润", "200");

            assertEquals("scatter", chartService.determineType(intent, result));
        }
    }

    @Nested
    @DisplayName("extractLabels / buildDatasets")
    class DataExtraction {

        @Test
        @DisplayName("should extract labels from first column")
        void shouldExtractLabels() {
            var result = makeResult("地区", "华东", "金额", "500");
            var labels = chartService.extractLabels(result);
            assertEquals(List.of("华东"), labels);
        }

        @Test
        @DisplayName("should build datasets from numeric columns")
        void shouldBuildDatasets() {
            var result = new QueryResult(
                    List.of("地区", "金额"),
                    List.of(
                            Map.of("地区", "华东", "金额", 500),
                            Map.of("地区", "华北", "金额", 300)
                    ),
                    2, 5, null, false, "test");

            var labels = chartService.extractLabels(result);
            var datasets = chartService.buildDatasets(result, labels);

            assertEquals(1, datasets.size());
            assertEquals("金额", datasets.get(0).label());
            assertEquals(List.of(500, 300), datasets.get(0).data());
            assertNotNull(datasets.get(0).color());
        }
    }

    @Nested
    @DisplayName("recommend — full pipeline")
    class FullRecommend {

        @Test
        @DisplayName("should return table spec for empty result")
        void shouldReturnTableForEmptyResult() {
            var intent = new IntentDTO("query", List.of(), List.of(),
                    List.of(), null, null, false, List.of());

            var spec = chartService.recommend(QueryResult.empty(), intent);

            assertEquals("table", spec.type());
            assertTrue(spec.datasets().isEmpty());
        }

        @Test
        @DisplayName("should return complete chart spec with title and datasets")
        void shouldReturnCompleteSpec() {
            var intent = new IntentDTO("aggregation", List.of("销售额"), List.of("地区"),
                    List.of(), null, null, false, List.of());
            var result = new QueryResult(
                    List.of("地区", "销售额"),
                    List.of(Map.of("地区", "华东", "销售额", 500)),
                    1, 5, null, false, "test");

            var spec = chartService.recommend(result, intent);

            assertNotNull(spec.type());
            assertEquals("销售额", spec.title());
            assertEquals(List.of("华东"), spec.labels());
            assertEquals(1, spec.datasets().size());
        }
    }
}
