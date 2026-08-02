package com.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds a WHITELIST, bounded audit detail snapshot.
 *
 * Constraint 3: never serialize the full request body / prompt / SQL / query result /
 * password / apiKeyRef. Only resource id, name, and necessary before/after differences.
 */
@Component
public class AuditDetailSanitizer {

    /** Keys never recorded, regardless of value. */
    private static final java.util.Set<String> BLOCKED_KEYS = java.util.Set.of(
            "apiKey", "api_key", "apiKeyRef", "api_key_ref", "token", "password",
            "secret", "api-key", "content", "prompt", "sql", "queryResult",
            "parameters", "variables", "baseUrl", "body");

    private final ObjectMapper objectMapper;

    public AuditDetailSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Whitelist-filter a structured map, keeping only safe keys (id/name/status).
     */
    public String safeDetail(Map<String, Object> raw) {
        if (raw == null) return null;
        Map<String, Object> safe = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            String key = e.getKey().toLowerCase();
            if (BLOCKED_KEYS.contains(key)) continue;
            // Only allow primitive-ish values (id, name, status, duration).
            Object v = e.getValue();
            if (v instanceof String || v instanceof Number || v instanceof Boolean || v == null) {
                safe.put(e.getKey(), v);
            }
        }
        try {
            return objectMapper.writeValueAsString(safe);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
