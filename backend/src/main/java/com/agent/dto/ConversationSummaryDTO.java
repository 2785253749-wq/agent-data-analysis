package com.agent.dto;

import java.time.LocalDateTime;

public record ConversationSummaryDTO(
        Long id,
        String title,
        String status,
        Long datasetId,
        Integer taskCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
