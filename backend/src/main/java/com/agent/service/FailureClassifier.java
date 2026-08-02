package com.agent.service;

import org.springframework.stereotype.Component;

/**
 * Maps a failed step (type + error message) to a STABLE failure category.
 * Categories are used for dashboard aggregation — never group by raw error text.
 */
@Component
public class FailureClassifier {

    public static final String SQL_VALIDATION = "SQL_VALIDATION";
    public static final String QUERY_EXECUTION = "QUERY_EXECUTION";
    public static final String MODEL_TIMEOUT = "MODEL_TIMEOUT";
    public static final String MODEL_RESPONSE = "MODEL_RESPONSE";
    public static final String UNEXPECTED = "UNEXPECTED";

    public String classify(String stepType, String errorMessage) {
        String msg = errorMessage == null ? "" : errorMessage.toLowerCase();
        String step = stepType == null ? "" : stepType;

        // Model timeout (HTTP timeout / read timeout)
        if (msg.contains("timeout") || msg.contains("timed out") || msg.contains("read timed out")
                || msg.contains("connect timed out")) {
            return MODEL_TIMEOUT;
        }

        // SQL validation failure (M3)
        if ("SQL_VALIDATE".equals(step) || msg.contains("sql validation failed")
                || msg.contains("violation")) {
            return SQL_VALIDATION;
        }

        // Query execution failure (M4)
        if ("QUERY".equals(step) || msg.contains("bad sql grammar")
                || msg.contains("sql syntax") || msg.contains("查询执行失败")) {
            return QUERY_EXECUTION;
        }

        // Model response malformed / empty / parse failure (M1/M2/M5)
        if (msg.contains("failed to parse") || msg.contains("empty response")
                || msg.contains("no choices") || msg.contains("deepseek call failed")
                || msg.contains("json parse")) {
            return MODEL_RESPONSE;
        }

        return UNEXPECTED;
    }
}
