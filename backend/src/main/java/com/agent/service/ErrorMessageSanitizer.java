package com.agent.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Sanitizes error messages before exposing them via API.
 *
 * Whitelist-first: recognize known business summaries and return them.
 * Fallback: regex-strip secrets, URLs, hosts, stacks.
 * Never exposes JDBC URL, DB account, IP, token, key, or raw stack trace.
 */
@Component
public class ErrorMessageSanitizer {

    /** Known business-prefix whitelist → safe summary template. */
    private static final List<Pattern> BUSINESS_PATTERNS = List.of(
            Pattern.compile("(?i)^SQL validation failed.*"),
            Pattern.compile("(?i)^查询执行失败.*"),
            Pattern.compile("(?i)^SQL生成失败.*"),
            Pattern.compile("(?i)^AI服务.*"),
            Pattern.compile("(?i)^DeepSeek.*")
    );

    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(api[-_]?key|token|secret|password|passwd)\\s*[=:]\\s*\\S+");
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._~+/=-]+");
    private static final Pattern IP_PATTERN = Pattern.compile("\\b\\d{1,3}(\\.\\d{1,3}){3}(:\\d{1,5})?\\b");
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)(?:https?|jdbc|mysql|h2)://\\S+");
    private static final Pattern STACK_LINE_PATTERN = Pattern.compile("(?m)^\\s*at\\s+.*$");

    /**
     * Sanitize a raw error message to a safe summary.
     * Returns a fixed generic message for null/blank input.
     */
    public String sanitize(String raw) {
        if (raw == null || raw.isBlank()) return "执行失败，原因未知";

        // Whitelist-first: known business summaries pass through (with secrets stripped defensively).
        for (Pattern p : BUSINESS_PATTERNS) {
            if (p.matcher(raw).find()) {
                return sanitizeGeneric(raw);
            }
        }

        // Fallback: strip everything sensitive, keep only the first meaningful line.
        String oneLine = firstMeaningfulLine(raw);
        return sanitizeGeneric(oneLine);
    }

    /**
     * Keep only the first non-empty line that is not a stack frame or Caused-by marker.
     */
    private String firstMeaningfulLine(String raw) {
        for (String line : raw.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            if (t.startsWith("at ") || t.startsWith("Caused by:")
                    || t.startsWith("Suppressed:") || t.startsWith("...")) {
                continue;
            }
            return t;
        }
        return raw;
    }

    /**
     * Strip secrets/URLs/IPs/stack lines and truncate to a bounded length.
     */
    private String sanitizeGeneric(String text) {
        String out = text;
        out = SECRET_PATTERN.matcher(out).replaceAll("$1=***");
        out = BEARER_PATTERN.matcher(out).replaceAll("Bearer ***");
        out = URL_PATTERN.matcher(out).replaceAll("[URL]");
        out = IP_PATTERN.matcher(out).replaceAll("[IP]");
        out = STACK_LINE_PATTERN.matcher(out).replaceAll("");
        out = out.replaceAll("\\s+", " ").trim();
        if (out.length() > 300) out = out.substring(0, 300) + "…";
        return out.isEmpty() ? "执行失败，原因未知" : out;
    }
}
