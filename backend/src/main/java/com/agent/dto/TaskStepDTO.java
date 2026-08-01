package com.agent.dto;

/**
 * One step within a task detail — status/duration only, no prompt content or keys.
 */
public record TaskStepDTO(
        String stepType,
        String status,
        Long durationMs,
        String errorMessage
) {}
