package com.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Create/update model request.
 * apiKeyRef must be a whitelist env var name (e.g. DEEPSEEK_API_KEY).
 */
public record AiModelRequest(
        @NotBlank String name,
        @NotBlank String provider,
        @NotBlank String baseUrl,
        @NotBlank String modelName,
        @NotNull Integer timeoutMs,
        @NotNull Double temperature,
        @NotNull Integer maxTokens,
        @NotBlank String apiKeyRef,
        Boolean isEnabled,
        Boolean isDefault
) {}
