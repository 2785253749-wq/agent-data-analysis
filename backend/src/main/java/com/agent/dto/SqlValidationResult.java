package com.agent.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of SQL safety validation.
 *
 * Per spec section 7: every AI-generated SQL must pass AST + permission + resource checks
 * before being executed.
 */
public record SqlValidationResult(
        boolean passed,
        String reason,
        List<String> violations,
        String sanitizedSql
) {
    public static SqlValidationResult pass(String sanitizedSql) {
        return new SqlValidationResult(true, "OK", List.of(), sanitizedSql);
    }

    public static SqlValidationResult reject(String reason, List<String> violations) {
        return new SqlValidationResult(false, reason, violations, null);
    }

    public static SqlValidationResult reject(String reason, String violation) {
        List<String> list = new ArrayList<>();
        list.add(violation);
        return new SqlValidationResult(false, reason, list, null);
    }
}
