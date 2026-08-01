package com.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromptTemplateDTO(
        Long id,
        String name,
        String type,
        Integer version,
        String content,
        String variables,
        String contentHash,
        String description,
        Boolean isEnabled,
        Boolean isArchived,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /** Create a new immutable version. */
    public record CreateRequest(
            String name,
            String type,
            String content,
            String variables,
            String description
    ) {}
}
