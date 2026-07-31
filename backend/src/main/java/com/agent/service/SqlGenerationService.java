package com.agent.service;

import com.agent.dto.*;
import com.agent.repository.DatasetFieldRepository;
import com.agent.repository.DatasetRepository;
import com.agent.repository.MetricsDefinitionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class SqlGenerationService {

    private static final Logger log = LoggerFactory.getLogger(SqlGenerationService.class);
    private final DeepSeekClient deepSeek;
    private final String systemPrompt;
    private final ObjectMapper objectMapper;
    private final DatasetRepository datasetRepo;
    private final DatasetFieldRepository fieldRepo;
    private final MetricsDefinitionRepository metricRepo;

    public SqlGenerationService(DeepSeekClient deepSeek, ObjectMapper objectMapper,
            DatasetRepository datasetRepo, DatasetFieldRepository fieldRepo,
            MetricsDefinitionRepository metricRepo) throws IOException {
        this.deepSeek = deepSeek;
        this.systemPrompt = new ClassPathResource("prompts/sql-generation/system.txt")
                .getContentAsString(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
        this.datasetRepo = datasetRepo;
        this.fieldRepo = fieldRepo;
        this.metricRepo = metricRepo;
    }

    public SqlResultDTO generate(SqlGenerationRequest request) {
        String context = buildContext(request.datasetId(), request.intent());
        String userMessage = buildUserMessage(request.question(), request.intent(), context);
        String jsonResponse = callModel(userMessage);
        return parseSqlResult(jsonResponse);
    }

    public SqlResultDTO parseSqlResult(String json) {
        try {
            String cleaned = cleanJson(json);
            return objectMapper.readValue(cleaned, SqlResultDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse SQL result JSON", e);
            throw new IllegalArgumentException("Failed to parse model response as SqlResultDTO", e);
        }
    }

    private String callModel(String userMessage) {
        try {
            return deepSeek.chat(systemPrompt, userMessage, 0.0);
        } catch (Exception e) {
            log.error("DeepSeek SQL generation failed", e);
            throw new RuntimeException("SQL生成失败: " + e.getMessage(), e);
        }
    }

    private String buildContext(Long datasetId, IntentDTO intent) {
        StringBuilder sb = new StringBuilder();
        datasetRepo.findById(datasetId).ifPresent(ds -> {
            sb.append("表名：").append(ds.getTableName()).append("（").append(ds.getName()).append("）\n\n字段：\n");
            for (var f : fieldRepo.findAllByDatasetId(datasetId)) {
                sb.append("  - ").append(f.getFieldName()).append(" | ").append(f.getDataType().name().toLowerCase());
                if (f.getFieldAlias() != null && !f.getFieldAlias().isBlank()) sb.append(" | ").append(f.getFieldAlias());
                if (f.getIsDimension()) sb.append(" | 维度");
                if (f.getIsMetric()) sb.append(" | 指标");
                sb.append("\n");
            }
            var metrics = metricRepo.findAllByDatasetId(datasetId);
            if (!metrics.isEmpty()) { sb.append("\n指标公式：\n"); for (var m : metrics) sb.append("  - ").append(m.getMetricName()).append("：").append(m.getFormula()).append("\n"); }
        });
        sb.append("\n意图：").append(intent.intentType());
        if (!intent.metrics().isEmpty()) sb.append("\n  指标：").append(String.join(", ", intent.metrics()));
        if (!intent.dimensions().isEmpty()) sb.append("\n  维度：").append(String.join(", ", intent.dimensions()));
        if (intent.timeRange() != null) sb.append("\n  时间：").append(intent.timeRange().type());
        return sb.toString();
    }

    private String buildUserMessage(String question, IntentDTO intent, String context) {
        return "用户问题：" + question + "\n意图类型：" + intent.intentType() + "\n\n数据集上下文：\n" + context + "\n\n请生成SQL（JSON格式）：";
    }

    private String cleanJson(String json) {
        String c = json.trim();
        if (c.startsWith("```json")) c = c.substring(7); else if (c.startsWith("```")) c = c.substring(3);
        if (c.endsWith("```")) c = c.substring(0, c.length() - 3);
        return c.trim();
    }
}
