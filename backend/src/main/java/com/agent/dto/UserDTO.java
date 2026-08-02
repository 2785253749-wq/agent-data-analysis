package com.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * User DTO — NEVER includes password or password hash.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDTO(
        Long id,
        String username,
        String displayName,
        String role,
        Long orgId,
        Boolean isEnabled,
        LocalDateTime createdAt
) {
    public record CreateRequest(
            String username,
            String password,
            String displayName,
            String role,
            Boolean isEnabled
    ) {}

    public record UpdateRequest(
            String displayName,
            String role,
            Boolean isEnabled
    ) {}
}
