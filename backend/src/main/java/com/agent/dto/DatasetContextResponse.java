package com.agent.dto;

import java.util.List;

/**
 * Aggregated dataset metadata for the Agent pipeline.
 *
 * GET /api/datasets/{id}/context
 *
 * This response provides all the metadata DeepSeek needs to generate correct SQL:
 * - dataset info (table name, org)
 * - field definitions (names, types, dimension/metric roles)
 * - metric formulas (standardized KPI calculations)
 */
public record DatasetContextResponse(
        DatasetResponse dataset,
        List<DatasetFieldResponse> fields,
        List<MetricsDefinitionResponse> metrics
) {}
