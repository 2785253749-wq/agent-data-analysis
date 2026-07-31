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
 * Generates SQL from structured intent and dataset metadata using DeepSeek.
 *
 * Per spec section 6.2: the model returns structured SQL JSON.
 * Output validation (AST check, field whitelist) is done by SqlSafetyService (M3).
 * This service only generates — it does NOT execute or validate SQL.
 */
@Service
public class SqlGenerationService {

    private static final Logger log = LoggerFactory.getLogger(SqlGenerationService.class);
    private static final String PROMPT_PATH = "prompts/sql-generation/system.txt";

    private final ChatClient chatClient;
    private final String systemPrompt;
    private final ObjectMapper objectMapper;
    private final DatasetRepository datasetRepo;
    private final DatasetFieldRepository fieldRepo;
    private final MetricsDefinitionRepository metricRepo;

    public SqlGenerationService(
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
     * Generate SQL from intent + dataset metadata.
     */
    public SqlResultDTO generate(SqlGenerationRequest request) {
        String context = buildContext(request.datasetId(), request.intent());
        String userMessage = buildUserMessage(request.question(), request.intent(), context);
        String jsonResponse = callModel(userMessage);
        return parseSqlResult(jsonResponse);
    }

    /**
     * Parse SQL result JSON. Public for testability.
     */
    public SqlResultDTO parseSqlResult(String json) {
        try {
            String cleaned = cleanJson(json);
            return objectMapper.readValue(cleaned, SqlResultDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse SQL result JSON: {}", json, e);
            throw new IllegalArgumentException("Failed to parse model response as SqlResultDTO", e);
        }
    }

    // ---- Private helpers ----

    private String loadPrompt() throws IOException {
        ClassPathResource resource = new ClassPathResource(PROMPT_PATH);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String buildContext(Long datasetId, IntentDTO intent) {
        StringBuilder sb = new StringBuilder();

        datasetRepo.findById(datasetId).ifPresent(ds -> {
            sb.append("表名：").append(ds.getTableName()).append("\n");
            sb.append("数据集：").append(ds.getName()).append("\n\n");

            List<DatasetFieldEntity> fields = fieldRepo.findAllByDatasetId(datasetId);
            if (!fields.isEmpty()) {
                sb.append("可用字段：\n");
                for (DatasetFieldEntity f : fields) {
                    sb.append("  - ").append(f.getFieldName())
                      .append(" | ").append(f.getDataType().name().toLowerCase());
                    if (f.getFieldAlias() != null && !f.getFieldAlias().isBlank()) {
                        sb.append(" | ").append(f.getFieldAlias());
                    }
                    if (f.getIsDimension()) sb.append(" | 维度");
                    if (f.getIsMetric()) sb.append(" | 指标");
                    sb.append("\n");
                }
            }

            List<MetricsDefinitionEntity> metrics = metricRepo.findAllByDatasetId(datasetId);
            if (!metrics.isEmpty()) {
                sb.append("\n可用指标公式：\n");
                for (MetricsDefinitionEntity m : metrics) {
                    sb.append("  - ").append(m.getMetricName())
                      .append("：").append(m.getFormula()).append("\n");
                }
            }
        });

        // Append intent
        sb.append("\n意图信息：\n");
        sb.append("  类型：").append(intent.intentType()).append("\n");
        if (!intent.metrics().isEmpty()) {
            sb.append("  指标：").append(String.join(", ", intent.metrics())).append("\n");
        }
        if (!intent.dimensions().isEmpty()) {
            sb.append("  维度：").append(String.join(", ", intent.dimensions())).append("\n");
        }
        if (intent.timeRange() != null) {
            sb.append("  时间范围：").append(intent.timeRange().type())
              .append(", start=").append(intent.timeRange().start())
              .append(", end=").append(intent.timeRange().end()).append("\n");
        }
        if (!intent.filters().isEmpty()) {
            sb.append("  过滤条件：\n");
            intent.filters().forEach(f ->
                sb.append("    - ").append(f.field()).append(" ")
                  .append(f.operator()).append(" ").append(f.value()).append("\n"));
        }
        if (intent.comparison() != null) {
            sb.append("  对比：").append(intent.comparison()).append("\n");
        }

        return sb.toString();
    }

    private String buildUserMessage(String question, IntentDTO intent, String context) {
        return """
                用户问题：%s
                意图类型：%s

                数据集上下文：
                %s

                请生成SQL（严格按照JSON格式输出，不要有任何其他文字）：
                """.formatted(question, intent.intentType(), context);
    }

    private String callModel(String userMessage) {
        try {
            String content = chatClient.prompt(new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userMessage)
            ))).call().content();
            log.debug("DeepSeek SQL response: {}", content);
            return content;
        } catch (Exception e) {
            log.warn("DeepSeek SQL call failed on first attempt: {}", e.getMessage());
            try {
                Thread.sleep(1000);
                var prompt = new Prompt(List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(userMessage)
                ));
                return chatClient.prompt(prompt).call().content();
            } catch (Exception e2) {
                log.error("DeepSeek SQL call failed on retry", e2);
                throw new RuntimeException("SQL生成服务暂时不可用，请稍后重试", e2);
            }
        }
    }

    private String cleanJson(String json) {
        String cleaned = json.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        return cleaned.trim();
    }
}
