package com.agent.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Conversation detail: summary + context (redacted) + linked tasks as turns.
 */
public record ConversationDetailDTO(
        Long id,
        String title,
        String status,
        Long datasetId,
        Integer taskCount,
        Object contextSummary,
        List<TurnDTO> turns,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /** One conversation turn = one linked analysis task. */
    public record TurnDTO(
            Long taskId,
            String question,
            String status,
            Long durationMs,
            LocalDateTime createdAt
    ) {}
}
