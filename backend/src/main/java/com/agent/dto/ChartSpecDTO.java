package com.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * ECharts-compatible chart specification.
 *
 * The frontend ChartRenderer consumes this spec directly to render charts.
 * Chart type is recommended by ChartRecommendationService based on intent type + data structure.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChartSpecDTO(

        /** Chart type: bar | line | pie | table | scatter | horizontal_bar */
        String type,

        /** Chart title */
        String title,

        /** X-axis labels (categories or time values) */
        List<String> labels,

        /** Datasets to plot */
        List<ChartDataset> datasets,

        /** Optional ECharts options overrides */
        Map<String, Object> options

) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChartDataset(
            String label,
            List<Number> data,
            String color       // hex color (optional)
    ) {}
}
