package com.agent.dto;

import com.agent.entity.MetricsDefinitionEntity;

import java.time.LocalDateTime;

/**
 * Response for metric definition CRUD operations.
 */
public record MetricsDefinitionResponse(
        Long id,
        Long datasetId,
        String metricName,
        String formula,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MetricsDefinitionResponse from(MetricsDefinitionEntity entity) {
        return new MetricsDefinitionResponse(
                entity.getId(),
                entity.getDatasetId(),
                entity.getMetricName(),
                entity.getFormula(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
