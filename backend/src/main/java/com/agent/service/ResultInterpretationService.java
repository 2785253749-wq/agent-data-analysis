package com.agent.service;

import com.agent.dto.InterpretationDTO;
import com.agent.dto.QueryResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Generates natural language interpretation of query results using DeepSeek.
 *
 * Per spec section 6.3: conclusions must have evidence, distinguish fact/inference/suggestion,
 * and never reference data not present in the query result.
 */
@Service
public class ResultInterpretationService {

    private static final Logger log = LoggerFactory.getLogger(ResultInterpretationService.class);
    private static final String PROMPT_PATH = "prompts/interpretation/system.txt";

    private final ChatClient chatClient;
    private final String systemPrompt;
    private final ObjectMapper objectMapper;

    public ResultInterpretationService(ChatClient chatClient, ObjectMapper objectMapper) throws IOException {
        this.chatClient = chatClient;
        this.systemPrompt = loadPrompt();
        this.objectMapper = objectMapper;
    }

    /**
     * Interpret query results in the context of the original question.
     */
    public InterpretationDTO interpret(String question, QueryResult result) {
        String context = buildContext(question, result);
        String jsonResponse = callModel(context);
        return parseInterpretation(jsonResponse);
    }

    /**
     * Parse interpretation JSON. Public for testability.
     */
    public InterpretationDTO parseInterpretation(String json) {
        try {
            String cleaned = cleanJson(json);
            return objectMapper.readValue(cleaned, InterpretationDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse interpretation JSON: {}", json, e);
            throw new IllegalArgumentException("Failed to parse model response as InterpretationDTO", e);
        }
    }

    // ---- Private helpers ----

    private String loadPrompt() throws IOException {
        return new ClassPathResource(PROMPT_PATH).getContentAsString(StandardCharsets.UTF_8);
    }

    private String buildContext(String question, QueryResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户问题：").append(question).append("\n\n");
        sb.append("查询结果摘要：\n");

        if (result.summary() != null) {
            sb.append(result.summary()).append("\n");
        }

        sb.append("列名：").append(String.join(", ", result.columns())).append("\n");
        sb.append("行数：").append(result.rowCount()).append("\n");

        if (!result.rows().isEmpty()) {
            sb.append("\n数据预览（前5行）：\n");
            int count = 0;
            for (var row : result.rows()) {
                if (count++ >= 5) break;
                sb.append("  ").append(row.toString()).append("\n");
            }
            if (result.rowCount() > 5) {
                sb.append("  ...（共").append(result.rowCount()).append("行）\n");
            }
        }

        if (result.truncated()) {
            sb.append("\n⚠️ 注意：结果已被截断，实际数据量可能更大。\n");
        }

        sb.append("\n请输出解释JSON（不要包含任何其他文字）：");
        return sb.toString();
    }

    private String callModel(String userMessage) {
        try {
            String content = chatClient.prompt(new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userMessage)
            ))).call().content();
            log.debug("DeepSeek interpretation: {}", content);
            return content;
        } catch (Exception e) {
            log.warn("DeepSeek interpretation failed, retrying: {}", e.getMessage());
            try {
                Thread.sleep(1000);
                return chatClient.prompt(new Prompt(List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(userMessage)
                ))).call().content();
            } catch (Exception e2) {
                log.error("DeepSeek interpretation failed on retry", e2);
                return buildFallbackResponse();
            }
        }
    }

    private String buildFallbackResponse() {
        return """
                {
                  "conclusion": "由于AI服务暂时不可用，无法生成数据解释",
                  "points": [],
                  "dataSufficient": false,
                  "confidence": "low",
                  "caveats": ["AI服务不可用，以下为原始数据结果"]
                }""";
    }

    private String cleanJson(String json) {
        String cleaned = json.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        return cleaned.trim();
    }
}
