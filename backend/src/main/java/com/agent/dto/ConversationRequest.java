package com.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create / rename / update a conversation.
 */
public record ConversationRequest(
        @NotBlank @Size(max = 200) String title,
        Long datasetId
) {}
