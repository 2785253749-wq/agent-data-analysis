package com.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Contract for GET /api/health.
 * Used by monitoring, load balancers, and frontend connectivity checks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealthResponse(
        String status,
        String application,
        String version,
        Instant timestamp,
        ComponentHealth database,
        ComponentHealth deepseek
) {

    public static HealthResponse up(String application, String version) {
        return new HealthResponse(
                "UP",
                application,
                version,
                Instant.now(),
                null,
                null
        );
    }

    public HealthResponse withDatabase(ComponentHealth db) {
        return new HealthResponse(
                this.status, this.application, this.version, this.timestamp,
                db, this.deepseek
        );
    }

    public HealthResponse withDeepseek(ComponentHealth ds) {
        return new HealthResponse(
                this.status, this.application, this.version, this.timestamp,
                this.database, ds
        );
    }

    /**
     * Sub-component health detail.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ComponentHealth(
            String status,
            String message
    ) {}
}
