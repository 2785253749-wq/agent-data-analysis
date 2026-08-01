package com.agent;

import com.agent.dto.IntentDTO;
import com.agent.service.ConversationContextService;
import com.agent.service.ErrorMessageSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConversationContextService")
class ConversationContextServiceTest {

    private ConversationContextService svc;

    @BeforeEach
    void setUp() {
        svc = new ConversationContextService(new ObjectMapper(), new ErrorMessageSanitizer());
    }

    @Test
    void shouldMergeMetricsAndDimensionsDeduped() {
        Map<String, Object> existing = new LinkedHashMap<>();
        existing.put("metrics", List.of("总销售额"));
        existing.put("dimensions", List.of("地区"));

        IntentDTO intent = new IntentDTO("aggregation",
                List.of("总销售额", "订单数"), List.of("地区", "月份"),
                List.of(), null, null, false, List.of());

        Map<String, Object> merged = svc.mergeCompletedIntent(
                existing, intent, 1L, "销售数据", "结论");

        assertEquals(2, ((List<?>) merged.get("metrics")).size());
        assertEquals(2, ((List<?>) merged.get("dimensions")).size());
        assertEquals("总销售额", ((List<?>) merged.get("metrics")).get(0));
    }

    @Test
    void shouldPreserveTimeRangeWhenIntentHasNone() {
        Map<String, Object> existing = new LinkedHashMap<>();
        existing.put("timeRange", Map.of("type", "latest_n_days", "start", "30"));

        // Follow-up like "那华东呢？" has no timeRange → previous preserved (补充点3)
        IntentDTO noTimeIntent = new IntentDTO("query", List.of(), List.of("地区"),
                List.of(), null, null, false, List.of());

        Map<String, Object> merged = svc.mergeCompletedIntent(existing, noTimeIntent, 1L, "销售数据", null);

        assertEquals("latest_n_days", ((Map<?, ?>) merged.get("timeRange")).get("type"));
    }

    @Test
    void shouldOverrideTimeRangeWhenIntentHasOne() {
        Map<String, Object> existing = new LinkedHashMap<>();
        existing.put("timeRange", Map.of("type", "latest_n_days", "start", "30"));

        IntentDTO withTime = new IntentDTO("aggregation", List.of("总销售额"), List.of("月份"),
                List.of(), new IntentDTO.TimeRangeDef("latest_n_months", "12", null),
                null, false, List.of());

        Map<String, Object> merged = svc.mergeCompletedIntent(existing, withTime, 1L, "销售数据", null);

        assertEquals("latest_n_months", ((Map<?, ?>) merged.get("timeRange")).get("type"));
    }

    @Test
    void shouldRedactAndCapLastConclusion() {
        Map<String, Object> existing = new LinkedHashMap<>();

        // Conclusion contains a secret → must be redacted (补充点4)
        String secretConclusion = "华东最高 api-key=sk-secret123 后面还有超长内容".repeat(60);

        Map<String, Object> merged = svc.mergeCompletedIntent(
                existing, null, 1L, "销售数据", secretConclusion);

        String conclusion = (String) merged.get("lastConclusion");
        assertNotNull(conclusion);
        assertFalse(conclusion.contains("sk-secret123"), "secret must be redacted");
        assertTrue(conclusion.length() <= 501, "conclusion capped");
    }
}
