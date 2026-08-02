package com.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditLogDTO(
        Long id,
        String operatorName,
        Long userId,
        String action,
        String resourceType,
        Long resourceId,
        String result,
        String detail,
        String ipAddress,
        LocalDateTime createdAt
) {}
