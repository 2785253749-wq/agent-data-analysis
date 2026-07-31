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
 */
@RestController
public class HealthController {

    private final String applicationName;
    private final String version;
    private final DataSource dataSource;
    private final String deepseekApiKey;

    public HealthController(
            @Value("${app.name}") String applicationName,
            @Value("${app.version}") String version,
            DataSource dataSource,
            @Value("${spring.ai.openai.api-key:}") String deepseekApiKey) {
        this.applicationName = applicationName;
        this.version = version;
        this.dataSource = dataSource;
        this.deepseekApiKey = deepseekApiKey;
    }

    @GetMapping("/api/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = HealthResponse.up(applicationName, version);
        response = response.withDatabase(checkDatabase());
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
        if (deepseekApiKey != null && !deepseekApiKey.isBlank() && !deepseekApiKey.startsWith("sk-your-")) {
            return new HealthResponse.ComponentHealth("UP", "API key configured");
        }
        return new HealthResponse.ComponentHealth("UNKNOWN", "API key not configured or using placeholder");
    }
}
