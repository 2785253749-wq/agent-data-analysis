package com.agent;

import com.agent.dto.InterpretationDTO;
import com.agent.service.ResultInterpretationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ResultInterpretationService")
class ResultInterpretationServiceTest {

    @Autowired
    private ResultInterpretationService interpretationService;

    @Nested
    @DisplayName("parseInterpretation — JSON parsing")
    class ParseInterpretation {

        @Test
        @DisplayName("should parse valid interpretation JSON")
        void shouldParseValidInterpretation() {
            String json = """
                    {
                      "conclusion": "上半年订单总额增长23.5%",
                      "points": [
                        {
                          "statement": "订单总额同比增长23.5%",
                          "type": "fact",
                          "evidence": "2026年上半年订单总额1,234万元",
                          "confidence": 0.95
                        },
                        {
                          "statement": "华东地区是主要增长驱动力",
                          "type": "inference",
                          "evidence": "华东增长35.2%，其他地区平均12.1%",
                          "confidence": 0.82
                        }
                      ],
                      "dataSufficient": true,
                      "confidence": "high",
                      "caveats": ["仅覆盖2025-2026年数据"]
                    }""";

            InterpretationDTO result = interpretationService.parseInterpretation(json);

            assertNotNull(result.conclusion());
            assertEquals(2, result.points().size());

            // First point is a fact
            assertEquals("fact", result.points().get(0).type());
            assertEquals(0.95, result.points().get(0).confidence(), 0.01);
            assertNotNull(result.points().get(0).evidence());

            // Second point is an inference
            assertEquals("inference", result.points().get(1).type());

            assertTrue(result.dataSufficient());
            assertEquals("high", result.confidence());
            assertEquals(1, result.caveats().size());
        }

        @Test
        @DisplayName("should parse interpretation with suggestion type")
        void shouldParseSuggestion() {
            String json = """
                    {
                      "conclusion": "建议增加库存储备",
                      "points": [
                        {
                          "statement": "建议增加华东地区库存20%",
                          "type": "suggestion",
                          "evidence": "华东地区订单增速35%超过全国平均",
                          "confidence": 0.65
                        }
                      ],
                      "dataSufficient": true,
                      "confidence": "medium",
                      "caveats": []
                    }""";

            InterpretationDTO result = interpretationService.parseInterpretation(json);
            assertEquals("suggestion", result.points().get(0).type());
        }

        @Test
        @DisplayName("should parse insufficient data interpretation")
        void shouldParseInsufficientData() {
            String json = """
                    {
                      "conclusion": "数据不足以得出有意义的结论",
                      "points": [],
                      "dataSufficient": false,
                      "confidence": "low",
                      "caveats": ["查询结果为空", "建议扩大时间范围"]
                    }""";

            InterpretationDTO result = interpretationService.parseInterpretation(json);

            assertFalse(result.dataSufficient());
            assertEquals("low", result.confidence());
            assertTrue(result.points().isEmpty());
        }

        @Test
        @DisplayName("should parse markdown-wrapped JSON")
        void shouldParseMarkdownWrapped() {
            String json = """
                    ```json
                    {
                      "conclusion": "测试结论",
                      "points": [],
                      "dataSufficient": true,
                      "confidence": "high",
                      "caveats": []
                    }
                    ```""";

            InterpretationDTO result = interpretationService.parseInterpretation(json);
            assertEquals("测试结论", result.conclusion());
        }

        @Test
        @DisplayName("should throw on invalid JSON")
        void shouldThrowOnInvalidJson() {
            assertThrows(IllegalArgumentException.class, () ->
                    interpretationService.parseInterpretation("not json"));
        }

        @Test
        @DisplayName("should throw on empty string")
        void shouldThrowOnEmptyString() {
            assertThrows(IllegalArgumentException.class, () ->
                    interpretationService.parseInterpretation(""));
        }

        @Test
        @DisplayName("should parse all three point types in one response")
        void shouldParseMixedTypes() {
            String json = """
                    {
                      "conclusion": "销售增长良好，建议继续投入",
                      "points": [
                        {"statement": "增长23%", "type": "fact", "evidence": "数据", "confidence": 0.95},
                        {"statement": "趋势将持续", "type": "inference", "evidence": "连续3月增长", "confidence": 0.70},
                        {"statement": "增加预算", "type": "suggestion", "evidence": "ROI正向", "confidence": 0.60}
                      ],
                      "dataSufficient": true,
                      "confidence": "medium",
                      "caveats": []
                    }""";

            InterpretationDTO result = interpretationService.parseInterpretation(json);
            assertEquals(3, result.points().size());
            assertEquals("fact", result.points().get(0).type());
            assertEquals("inference", result.points().get(1).type());
            assertEquals("suggestion", result.points().get(2).type());
        }
    }
}
