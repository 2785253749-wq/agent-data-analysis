package com.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a metric definition.
 *
 * POST /api/admin/datasets/{datasetId}/metrics
 * PUT  /api/admin/datasets/{datasetId}/metrics/{metricId}
 */
public record MetricsDefinitionRequest(

        @NotBlank(message = "指标名称不能为空")
        @Size(max = 200, message = "指标名称最长200字符")
        String metricName,

        @NotBlank(message = "计算公式不能为空")
        String formula,

        String description
) {}
