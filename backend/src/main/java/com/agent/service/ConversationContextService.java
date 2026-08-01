package com.agent.service;

import com.agent.dto.IntentDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds / merges the structured conversation context summary.
 *
 * Rules (per review feedback):
 * - Store summary only, never concatenate full history.
 * - metrics/dimensions dedupe-merge; keep up to a bounded count.
 * - timeRange only updates when the current intent EXPLICITLY sets one (a follow-up like
 *   "那华东呢？" preserves the previous timeRange).
 * - lastConclusion is REDACTED (field-level) and length-capped before storing.
 * - taskCount counts ALL created tasks (incl. failed); context only updates on COMPLETED.
 */
@Component
public class ConversationContextService {

    private static final Logger log = LoggerFactory.getLogger(ConversationContextService.class);
    private static final int MAX_METRICS = 10;
    private static final int MAX_DIMENSIONS = 10;
    private static final int MAX_CONCLUSION_LEN = 500;

    private final ObjectMapper objectMapper;
    private final ErrorMessageSanitizer sanitizer;

    public ConversationContextService(ObjectMapper objectMapper, ErrorMessageSanitizer sanitizer) {
        this.objectMapper = objectMapper;
        this.sanitizer = sanitizer;
    }

    /** Parse the stored context JSON (may be null/blank → empty map). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            Object o = objectMapper.readValue(json, Object.class);
            return (Map<String, Object>) o;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse context summary: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    public String toJson(Map<String, Object> ctx) {
        try { return objectMapper.writeValueAsString(ctx); }
        catch (JsonProcessingException e) { return "{}"; }
    }

    /**
     * Merge a completed analysis intent into the existing context.
     * Returns the merged map. Does NOT touch taskCount (handled by service).
     */
    public Map<String, Object> mergeCompletedIntent(Map<String, Object> existing, IntentDTO intent,
                                                    Long datasetId, String datasetName,
                                                    String lastConclusion) {
        Map<String, Object> ctx = new LinkedHashMap<>(existing);

        // Dataset (only set on first turn; subsequent dataset changes rejected earlier)
        ctx.putIfAbsent("datasetId", datasetId);
        ctx.putIfAbsent("datasetName", datasetName);

        // metrics/dimensions — dedupe merge, bounded
        ctx.put("metrics", mergeList(ctx.get("metrics"), intent != null ? intent.metrics() : null));
        ctx.put("dimensions", mergeList(ctx.get("dimensions"), intent != null ? intent.dimensions() : null));

        // timeRange — only update if intent EXPLICITLY has one (补充点3)
        if (intent != null && intent.timeRange() != null) {
            ctx.put("timeRange", Map.of(
                    "type", String.valueOf(intent.timeRange().type()),
                    "start", String.valueOf(intent.timeRange().start()),
                    "end", String.valueOf(intent.timeRange().end())));
        }

        // lastConclusion — redacted + capped (补充点4)
        if (lastConclusion != null && !lastConclusion.isBlank()) {
            String safe = sanitizer.sanitize(lastConclusion);
            if (safe.length() > MAX_CONCLUSION_LEN) {
                safe = safe.substring(0, MAX_CONCLUSION_LEN) + "…";
            }
            ctx.put("lastConclusion", safe);
        }

        return ctx;
    }

    /** Redact-sensitive & bounded list merge. */
    private List<String> mergeList(Object existing, List<String> incoming) {
        Set<String> set = new LinkedHashSet<>();
        if (existing instanceof List<?> list) {
            for (Object o : list) if (o != null) set.add(String.valueOf(o));
        }
        if (incoming != null) {
            for (String s : incoming) if (s != null && !s.isBlank()) set.add(s);
        }
        List<String> out = new ArrayList<>(set);
        return out.size() > Math.max(MAX_METRICS, MAX_DIMENSIONS)
                ? out.subList(0, Math.max(MAX_METRICS, MAX_DIMENSIONS)) : out;
    }
}
