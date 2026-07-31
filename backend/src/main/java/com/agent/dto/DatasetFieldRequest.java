package com.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a dataset field.
 *
 * POST /api/admin/datasets/{datasetId}/fields
 * PUT  /api/admin/datasets/{datasetId}/fields/{fieldId}
 */
public record DatasetFieldRequest(

        @NotBlank(message = "字段名不能为空")
        @Size(max = 200, message = "字段名最长200字符")
        String fieldName,

        @Size(max = 200, message = "字段别名最长200字符")
        String fieldAlias,

        @NotBlank(message = "数据类型不能为空")
        String dataType,

        Boolean isDimension,

        Boolean isMetric,

        Boolean isFilterable,

        String description
) {}
