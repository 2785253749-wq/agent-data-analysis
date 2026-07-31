package com.agent.service;

import com.agent.dto.*;
import com.agent.entity.DatasetFieldEntity;
import com.agent.entity.MetricsDefinitionEntity;
import com.agent.repository.DatasetFieldRepository;
import com.agent.repository.DatasetRepository;
import com.agent.repository.MetricsDefinitionRepository;
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
 * Calls DeepSeek to recognize user intent from natural language questions.
 *
 * Per spec section 6.1: the model returns structured intent JSON.
 * If parsing fails or the model returns invalid JSON, we retry once,
 * then fall back to a needsClarification response.
 */
@Service
public class IntentRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(IntentRecognitionService.class);
    private static final String PROMPT_PATH = "prompts/intent-recognition/system.txt";

    private final ChatClient chatClient;
    private final String systemPrompt;
    private final ObjectMapper objectMapper;
    private final DatasetRepository datasetRepo;
    private final DatasetFieldRepository fieldRepo;
    private final MetricsDefinitionRepository metricRepo;

    public IntentRecognitionService(
            ChatClient chatClient,
            ObjectMapper objectMapper,
            DatasetRepository datasetRepo,
            DatasetFieldRepository fieldRepo,
            MetricsDefinitionRepository metricRepo) throws IOException {
        this.chatClient = chatClient;
        this.systemPrompt = loadPrompt();
        this.objectMapper = objectMapper;
        this.datasetRepo = datasetRepo;
        this.fieldRepo = fieldRepo;
        this.metricRepo = metricRepo;
    }

    /**
     * Recognize intent from a natural language question.
     *
     * @param request question + optional datasetId for context
     * @return parsed IntentDTO with structured intent
     */
    public IntentDTO recognize(IntentRequest request) {
        // Build context string
        String context = buildContext(request.datasetId());

        // Build user message
        String userMessage = buildUserMessage(request.question(), context);

        // Call DeepSeek with retry
        String jsonResponse = callModel(userMessage);

        // Parse JSON
        return parseIntent(jsonResponse);
    }

    /**
     * Parse intent JSON string into IntentDTO.
     * Public for testability — allows testing parsing logic without calling DeepSeek.
     *
     * @param json raw JSON string from the model
     * @return parsed IntentDTO
     * @throws IllegalArgumentException if JSON is invalid
     */
    public IntentDTO parseIntent(String json) {
        try {
            // Strip markdown code fences if present
            String cleaned = json.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            return objectMapper.readValue(cleaned, IntentDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse intent JSON: {}", json, e);
            throw new IllegalArgumentException("Failed to parse model response as IntentDTO", e);
        }
    }

    // ---- Private helpers ----

    private String loadPrompt() throws IOException {
        ClassPathResource resource = new ClassPathResource(PROMPT_PATH);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String buildContext(Long datasetId) {
        if (datasetId == null) {
            return "（未指定数据集，请基于用户问题推断）";
        }

        StringBuilder sb = new StringBuilder();
        datasetRepo.findById(datasetId).ifPresent(ds -> {
            sb.append("数据集：").append(ds.getName())
              .append("（表名：").append(ds.getTableName()).append("）\n\n");

            List<DatasetFieldEntity> fields = fieldRepo.findAllByDatasetId(datasetId);
            if (!fields.isEmpty()) {
                sb.append("可用字段：\n");
                for (DatasetFieldEntity f : fields) {
                    sb.append("  - ").append(f.getFieldName());
                    if (f.getFieldAlias() != null && !f.getFieldAlias().isBlank()) {
                        sb.append("（").append(f.getFieldAlias()).append("）");
                    }
                    sb.append(" | 类型：").append(f.getDataType().name().toLowerCase());
                    if (f.getIsDimension()) sb.append(" | 维度");
                    if (f.getIsMetric()) sb.append(" | 指标");
                    sb.append("\n");
                }
            }

            List<MetricsDefinitionEntity> metrics = metricRepo.findAllByDatasetId(datasetId);
            if (!metrics.isEmpty()) {
                sb.append("\n可用指标：\n");
                for (MetricsDefinitionEntity m : metrics) {
                    sb.append("  - ").append(m.getMetricName())
                      .append("：").append(m.getFormula()).append("\n");
                }
            }
        });
        return sb.toString();
    }

    private String buildUserMessage(String question, String context) {
        return """
                用户问题：%s

                数据集上下文：
                %s

                请输出意图JSON（不要包含任何其他文字）：
                """.formatted(question, context);
    }

    private String callModel(String userMessage) {
        // First attempt
        try {
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userMessage)
            ));
            String content = chatClient.prompt(prompt).call().content();
            log.debug("DeepSeek intent response: {}", content);
            return content;
        } catch (Exception e) {
            log.warn("DeepSeek call failed on first attempt: {}", e.getMessage());
            // Retry once
            try {
                Thread.sleep(1000);
                Prompt prompt = new Prompt(List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(userMessage)
                ));
                var response = chatClient.prompt(prompt).call();
                return response.content();
            } catch (Exception e2) {
                log.error("DeepSeek call failed on retry", e2);
                return buildFallbackResponse();
            }
        }
    }

    /**
     * When all DeepSeek calls fail, return a fallback response asking for clarification.
     * This is the safest default — never fabricate intent.
     */
    private String buildFallbackResponse() {
        return """
                {
                  "intentType": "query",
                  "metrics": [],
                  "dimensions": [],
                  "filters": [],
                  "timeRange": null,
                  "comparison": null,
                  "needsClarification": true,
                  "clarificationQuestions": ["抱歉，AI服务暂时不可用，请稍后重试或重新描述您的问题"]
                }""";
    }
}
