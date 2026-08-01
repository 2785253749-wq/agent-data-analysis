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

import com.agent.entity.DatasetFieldEntity;
import com.agent.repository.DatasetFieldRepository;
import com.agent.service.ConversationContextService;
import com.agent.service.SensitiveDataMasker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ConversationContextService")
class ConversationContextServiceTest {

    private ConversationContextService svc;
    private DatasetFieldRepository fieldRepo;

    @BeforeEach
    void setUp() {
        fieldRepo = mock(DatasetFieldRepository.class);
        svc = new ConversationContextService(new ObjectMapper(), new SensitiveDataMasker(), fieldRepo);
    }

    private void seedSensitiveField(Long datasetId, String name, String alias) {
        DatasetFieldEntity f = new DatasetFieldEntity();
        f.setDatasetId(datasetId);
        f.setFieldName(name);
        f.setFieldAlias(alias);
        f.setIsSensitive(true);
        when(fieldRepo.findAllByDatasetId(datasetId)).thenReturn(List.of(f));
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

        // Conclusion contains PII — must be masked (手机号) + capped (补充点4)
        String piiConclusion = "华东最高 手机号13812345678 后面还有超长内容".repeat(60);

        Map<String, Object> merged = svc.mergeCompletedIntent(
                existing, null, 1L, "销售数据", piiConclusion);

        String conclusion = (String) merged.get("lastConclusion");
        assertNotNull(conclusion);
        assertFalse(conclusion.contains("13812345678"), "phone must be masked");
        assertTrue(conclusion.length() <= 501, "conclusion capped");
    }

    @Test
    void shouldMaskPhoneNumberInConclusion() {
        seedSensitiveField(1L, "phone", "手机号");
        Map<String, Object> merged = svc.mergeCompletedIntent(
                new LinkedHashMap<>(), null, 1L, "销售数据",
                "最高联系人电话13812345678，次高13600000000");
        String conclusion = (String) merged.get("lastConclusion");
        assertNotNull(conclusion);
        assertFalse(conclusion.contains("13812345678"));
        assertFalse(conclusion.contains("13600000000"));
    }

    @Test
    void shouldMaskEmailInConclusion() {
        Map<String, Object> merged = svc.mergeCompletedIntent(
                new LinkedHashMap<>(), null, 1L, "销售数据",
                "客户联系 test.user@example.com 反馈");
        String conclusion = (String) merged.get("lastConclusion");
        assertNotNull(conclusion);
        assertFalse(conclusion.contains("test.user@example.com"));
    }

    @Test
    void shouldMaskIdCardAndAccountInConclusion() {
        Map<String, Object> merged = svc.mergeCompletedIntent(
                new LinkedHashMap<>(), null, 1L, "销售数据",
                "身份证110101199003077777 账号6222020200001234567");
        String conclusion = (String) merged.get("lastConclusion");
        assertNotNull(conclusion);
        assertFalse(conclusion.contains("110101199003077777"));
        assertFalse(conclusion.contains("6222020200001234567"));
    }

    @Test
    void shouldMaskSensitiveFieldLabeledValue() {
        seedSensitiveField(1L, "phone", "手机号");
        Map<String, Object> merged = svc.mergeCompletedIntent(
                new LinkedHashMap<>(), null, 1L, "销售数据",
                "手机号: 13812345678 的用户");
        String conclusion = (String) merged.get("lastConclusion");
        assertNotNull(conclusion);
        assertFalse(conclusion.contains("13812345678"));
    }
}
