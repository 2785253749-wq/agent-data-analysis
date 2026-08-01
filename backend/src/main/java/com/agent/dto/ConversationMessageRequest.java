package com.agent.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Follow-up question within a conversation.
 * datasetId optional — falls back to the conversation's dataset when null.
 */
public record ConversationMessageRequest(
        @NotBlank String question,
        Long datasetId
) {}
