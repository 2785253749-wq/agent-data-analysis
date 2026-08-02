package com.agent;

import com.agent.service.FailureClassifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FailureClassifier")
class FailureClassifierTest {

    private final FailureClassifier c = new FailureClassifier();

    @Test
    void classifiesSqlValidation() {
        assertEquals("SQL_VALIDATION", c.classify("SQL_VALIDATE", "SQL validation failed: bad"));
    }

    @Test
    void classifiesQueryExecution() {
        assertEquals("QUERY_EXECUTION", c.classify("QUERY", "bad SQL grammar [SELECT]"));
    }

    @Test
    void classifiesModelTimeout() {
        assertEquals("MODEL_TIMEOUT", c.classify("INTENT", "Read timed out after 30000ms"));
    }

    @Test
    void classifiesModelResponse() {
        assertEquals("MODEL_RESPONSE", c.classify("SQL_GEN", "Failed to parse model response"));
    }

    @Test
    void defaultsToUnexpected() {
        assertEquals("UNEXPECTED", c.classify("CHART", "weird error"));
    }
}
