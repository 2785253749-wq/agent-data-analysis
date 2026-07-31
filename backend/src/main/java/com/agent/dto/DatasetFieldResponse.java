package com.agent.dto;

import com.agent.entity.DatasetFieldEntity;

import java.time.LocalDateTime;

/**
 * Response for dataset field CRUD operations.
 */
public record DatasetFieldResponse(
        Long id,
        Long datasetId,
        String fieldName,
        String fieldAlias,
        String dataType,
        Boolean isDimension,
        Boolean isMetric,
        Boolean isFilterable,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DatasetFieldResponse from(DatasetFieldEntity entity) {
        return new DatasetFieldResponse(
                entity.getId(),
                entity.getDatasetId(),
                entity.getFieldName(),
                entity.getFieldAlias(),
                entity.getDataType().name().toLowerCase(),
                entity.getIsDimension(),
                entity.getIsMetric(),
                entity.getIsFilterable(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
