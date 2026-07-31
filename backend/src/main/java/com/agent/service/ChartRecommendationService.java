package com.agent.service;

import com.agent.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Recommends chart types and generates ECharts-compatible chart specs.
 *
 * Uses rule-based logic (deterministic, no DeepSeek call needed):
 * - aggregation + 1 time dimension + 1 metric → line chart
 * - aggregation + categorical dimension → bar chart
 * - aggregation + 1 categorical dimension + 1 metric → pie chart
 * - ranking → horizontal bar chart
 * - comparison → bar chart
 * - detail/query → table
 * - correlation / scatter-friendly data → scatter chart
 */
@Service
public class ChartRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(ChartRecommendationService.class);

    private static final List<String> CHART_COLORS = List.of(
            "#5470c6", "#91cc75", "#fac858", "#ee6666", "#73c0de",
            "#3ba272", "#fc8452", "#9a60b4", "#ea7ccc", "#48b8d0");

    /**
     * Generate chart spec from query result and intent.
     */
    public ChartSpecDTO recommend(QueryResult result, IntentDTO intent) {
        if (result == null || result.rows().isEmpty()) {
            return new ChartSpecDTO("table", "查询结果为空", List.of(), List.of(), Map.of());
        }

        String chartType = determineType(intent, result);
        String title = intent.metrics().isEmpty()
                ? "数据展示"
                : String.join("、", intent.metrics());

        // Extract labels (first dimension or first column)
        List<String> labels = extractLabels(result);

        // Build datasets (one per metric or numeric column)
        List<ChartSpecDTO.ChartDataset> datasets = buildDatasets(result, labels);

        Map<String, Object> options = buildOptions(chartType);

        log.debug("Chart recommended: type={} for intent={}", chartType, intent.intentType());
        return new ChartSpecDTO(chartType, title, labels, datasets, options);
    }

    // ---- Public for testability ----

    public String determineType(IntentDTO intent, QueryResult result) {
        String intentType = intent.intentType();
        int metricCount = countNumericColumns(result);
        int dimensionCount = intent.dimensions().size();

        // Ranking → horizontal bar
        if ("ranking".equals(intentType)) {
            return "horizontal_bar";
        }

        // Comparison → bar chart
        if ("comparison".equals(intentType)) {
            return "bar";
        }

        // Detail → table
        if ("detail".equals(intentType) || "query".equals(intentType)) {
            return "table";
        }

        // Correlation → scatter (need 2+ numeric columns total)
        if ("correlation".equals(intentType)) {
            int totalNumeric = countAllNumericColumns(result);
            return totalNumeric >= 2 ? "scatter" : "bar";
        }

        // Aggregation — choose based on dimensions
        if ("aggregation".equals(intentType)) {
            // If there's a time-like dimension, use line
            if (hasTimeDimension(result, intent)) {
                return "line";
            }
            // If single dimension + single metric, pie works well
            if (dimensionCount <= 1 && metricCount == 1 && result.rowCount() <= 10) {
                return "pie";
            }
            return "bar";
        }

        // Default: bar
        return "bar";
    }

    public List<String> extractLabels(QueryResult result) {
        if (result.columns().isEmpty()) return List.of();

        // First column is usually the label (dimension)
        String labelCol = result.columns().get(0);
        return result.rows().stream()
                .map(row -> String.valueOf(row.getOrDefault(labelCol, "")))
                .collect(Collectors.toList());
    }

    public List<ChartSpecDTO.ChartDataset> buildDatasets(QueryResult result, List<String> labels) {
        List<ChartSpecDTO.ChartDataset> datasets = new ArrayList<>();

        if (result.columns().size() <= 1) return datasets;

        // Each column after the first that is numeric becomes a dataset
        for (int i = 1; i < result.columns().size(); i++) {
            String colName = result.columns().get(i);
            List<Number> data = new ArrayList<>();
            for (var row : result.rows()) {
                Object val = row.get(colName);
                if (val instanceof Number n) {
                    data.add(n);
                } else if (val != null) {
                    try {
                        data.add(Double.parseDouble(val.toString()));
                    } catch (NumberFormatException e) {
                        data.add(0);
                    }
                } else {
                    data.add(0);
                }
            }
            String color = CHART_COLORS.get((i - 1) % CHART_COLORS.size());
            datasets.add(new ChartSpecDTO.ChartDataset(colName, data, color));
        }

        return datasets;
    }

    // ---- Private helpers ----

    private int countAllNumericColumns(QueryResult result) {
        if (result.rows().isEmpty()) return 0;
        int count = 0;
        for (String col : result.columns()) {
            for (var row : result.rows()) {
                if (row.get(col) instanceof Number) { count++; break; }
            }
        }
        return count;
    }

    private int countNumericColumns(QueryResult result) {
        if (result.rows().isEmpty() || result.columns().size() <= 1) return 0;
        int count = 0;
        for (int i = 1; i < result.columns().size(); i++) {
            String col = result.columns().get(i);
            for (var row : result.rows()) {
                Object val = row.get(col);
                if (val instanceof Number) { count++; break; }
            }
        }
        return count;
    }

    private boolean hasTimeDimension(QueryResult result, IntentDTO intent) {
        // Heuristic: check if a dimension name or timeRange indicates time
        if (intent.timeRange() != null) return true;
        for (String dim : intent.dimensions()) {
            String lower = dim.toLowerCase();
            if (lower.contains("时间") || lower.contains("日期") || lower.contains("月")
                    || lower.contains("年") || lower.contains("日") || lower.contains("time")
                    || lower.contains("date") || lower.contains("month") || lower.contains("year")) {
                return true;
            }
        }
        // Check first column labels for date patterns
        if (!result.rows().isEmpty()) {
            Object firstVal = result.rows().get(0).values().iterator().next();
            if (firstVal != null && firstVal.toString().matches(".*\\d{4}[-/]\\d{2}.*")) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> buildOptions(String chartType) {
        return Map.of(
                "animation", true,
                "responsive", true
        );
    }
}
