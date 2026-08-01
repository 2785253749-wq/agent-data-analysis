package com.agent;

import com.agent.service.ErrorMessageSanitizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorMessageSanitizer")
class ErrorMessageSanitizerTest {

    private final ErrorMessageSanitizer sanitizer = new ErrorMessageSanitizer();

    @Test
    void shouldStripApiKeySecret() {
        String out = sanitizer.sanitize("call failed with api-key=sk-abc123 token=tok");
        assertFalse(out.contains("sk-abc123"));
        assertFalse(out.contains("token=tok"));
    }

    @Test
    void shouldStripJdbcUrlAndDbAccount() {
        String out = sanitizer.sanitize("Failed to connect jdbc:mysql://db.example.com:3306/app?user=admin&password=pw");
        assertFalse(out.contains("db.example.com"));
        assertFalse(out.contains("password=pw"));
        assertFalse(out.contains("admin"));
    }

    @Test
    void shouldStripIpWithPort() {
        String out = sanitizer.sanitize("connection to 10.0.0.5:3306 refused");
        assertFalse(out.contains("10.0.0.5"));
    }

    @Test
    void shouldStripBearerToken() {
        String out = sanitizer.sanitize("Authorization Bearer eyJhbGciOiJIUzI1NiJ9.x");
        assertFalse(out.contains("eyJhbGciOiJIUzI1NiJ9"));
    }

    @Test
    void shouldKeepBusinessSummary() {
        String out = sanitizer.sanitize("SQL validation failed: Field 'secret_col' not in dataset whitelist");
        assertTrue(out.contains("SQL validation failed"));
    }

    @Test
    void shouldStripStackTraceFrames() {
        String raw = "查询执行失败: bad SQL\n\tat com.agent.service.QueryExecutionService.execute(QueryExecutionService.java:55)\n\tat com.agent...";
        String out = sanitizer.sanitize(raw);
        assertTrue(out.contains("查询执行失败"));
        assertFalse(out.contains("at com.agent"));
    }

    @Test
    void shouldReturnGenericForBlank() {
        assertEquals("执行失败，原因未知", sanitizer.sanitize(""));
        assertEquals("执行失败，原因未知", sanitizer.sanitize(null));
    }
}
