package com.agent.dto;

import com.agent.entity.DatasetEntity;

import java.time.LocalDateTime;

/**
 * Response for dataset CRUD operations.
 */
public record DatasetResponse(
        Long id,
        String name,
        String description,
        String tableName,
        Long orgId,
        Boolean isEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DatasetResponse from(DatasetEntity entity) {
        return new DatasetResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getTableName(),
                entity.getOrgId(),
                entity.getIsEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
