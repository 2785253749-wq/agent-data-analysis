package com.agent.dto;

import java.time.LocalDateTime;

/**
 * AI model config DTO.
 * apiKeyRef is NEVER exposed — only apiKeyConfigured boolean (constraint 4).
 */
public record AiModelDTO(
        Long id,
        String name,
        String provider,
        String baseUrl,
        String modelName,
        Integer timeoutMs,
        Double temperature,
        Integer maxTokens,
        Boolean isEnabled,
        Boolean isDefault,
        Boolean apiKeyConfigured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
