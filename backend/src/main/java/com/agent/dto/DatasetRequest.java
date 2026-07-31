package com.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a dataset.
 *
 * POST /api/admin/datasets
 * PUT  /api/admin/datasets/{id}
 */
public record DatasetRequest(

        @NotBlank(message = "数据集名称不能为空")
        @Size(max = 200, message = "名称最长200字符")
        String name,

        String description,

        @NotBlank(message = "表名不能为空")
        @Size(max = 200, message = "表名最长200字符")
        @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$",
                message = "表名必须以字母或下划线开头，只能包含字母、数字和下划线")
        String tableName,

        @NotNull(message = "组织ID不能为空")
        Long orgId,

        Boolean isEnabled
) {}
