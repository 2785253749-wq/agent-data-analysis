package com.agent.controller;

import com.agent.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Public health-check endpoint.
 *
 * GET /api/health → 200 { status: "UP", ... }
 *
 * Contract:
 * - Always returns 200 when the application is running.
 * - Database status reflects connectivity to the primary datasource.
 * - DeepSeek status reflects API key configuration (not an actual API call at this stage).
 * - Version matches pom.xml / app.version property.
 */
@RestController
public class HealthController {

    private final String applicationName;
    private final String version;
    private final DataSource dataSource;

    public HealthController(
            @Value("${app.name}") String applicationName,
            @Value("${app.version}") String version,
            DataSource dataSource) {
        this.applicationName = applicationName;
        this.version = version;
        this.dataSource = dataSource;
    }

    @GetMapping("/api/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = HealthResponse.up(applicationName, version);

        // Check database connectivity
        response = response.withDatabase(checkDatabase());

        // Check DeepSeek configuration (no network call at this stage)
        response = response.withDeepseek(checkDeepseek());

        return ResponseEntity.ok(response);
    }

    private HealthResponse.ComponentHealth checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(3)) {
                return new HealthResponse.ComponentHealth("UP", "Connected");
            }
            return new HealthResponse.ComponentHealth("DOWN", "Connection invalid");
        } catch (Exception e) {
            return new HealthResponse.ComponentHealth("DOWN", e.getMessage());
        }
    }

    private HealthResponse.ComponentHealth checkDeepseek() {
        // At T01 stage, only verify that an API key is configured.
        // Actual connectivity will be verified in a later task (e.g., M1).
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("sk-your-")) {
            return new HealthResponse.ComponentHealth("UP", "API key configured");
        }
        return new HealthResponse.ComponentHealth("UNKNOWN", "API key not configured or using placeholder");
    }
}
