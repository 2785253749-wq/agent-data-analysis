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
public class IntentRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(IntentRecognitionService.class);
    private final DeepSeekClient deepSeek;
    private final String systemPrompt;
    private final ObjectMapper objectMapper;
    private final DatasetRepository datasetRepo;
    private final DatasetFieldRepository fieldRepo;
    private final MetricsDefinitionRepository metricRepo;

    public IntentRecognitionService(DeepSeekClient deepSeek, ObjectMapper objectMapper,
            DatasetRepository datasetRepo, DatasetFieldRepository fieldRepo,
            MetricsDefinitionRepository metricRepo) throws IOException {
        this.deepSeek = deepSeek;
        this.systemPrompt = new ClassPathResource("prompts/intent-recognition/system.txt")
                .getContentAsString(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
        this.datasetRepo = datasetRepo;
        this.fieldRepo = fieldRepo;
        this.metricRepo = metricRepo;
    }

    public IntentDTO recognize(IntentRequest request) {
        String context = buildContext(request.datasetId());
        String userMessage = buildUserMessage(request.question(), context);
        String jsonResponse = callModel(userMessage);
        return parseIntent(jsonResponse);
    }

    public IntentDTO parseIntent(String json) {
        try {
            String cleaned = cleanJson(json);
            return objectMapper.readValue(cleaned, IntentDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse intent JSON: {}", json, e);
            throw new IllegalArgumentException("Failed to parse model response as IntentDTO", e);
        }
    }

    private String callModel(String userMessage) {
        try {
            return deepSeek.chat(systemPrompt, userMessage, 0.1);
        } catch (Exception e) {
            log.warn("DeepSeek intent call failed: {}", e.getMessage());
            return buildFallbackResponse();
        }
    }

    private String buildContext(Long datasetId) { /* same as before - omitted for brevity */
        if (datasetId == null) return "（未指定数据集）";
        StringBuilder sb = new StringBuilder();
        datasetRepo.findById(datasetId).ifPresent(ds -> {
            sb.append("数据集：").append(ds.getName()).append("（表名：").append(ds.getTableName()).append("）\n\n字段：\n");
            for (var f : fieldRepo.findAllByDatasetId(datasetId)) {
                sb.append("  - ").append(f.getFieldName());
                if (f.getFieldAlias() != null && !f.getFieldAlias().isBlank()) sb.append("（").append(f.getFieldAlias()).append("）");
                sb.append(" | ").append(f.getDataType().name().toLowerCase());
                if (f.getIsDimension()) sb.append(" | 维度");
                if (f.getIsMetric()) sb.append(" | 指标");
                sb.append("\n");
            }
            var metrics = metricRepo.findAllByDatasetId(datasetId);
            if (!metrics.isEmpty()) { sb.append("\n指标：\n"); for (var m : metrics) sb.append("  - ").append(m.getMetricName()).append("：").append(m.getFormula()).append("\n"); }
        });
        return sb.toString();
    }

    private String buildUserMessage(String question, String context) {
        return "用户问题：" + question + "\n\n数据集上下文：\n" + context + "\n\n请输出意图JSON：";
    }

    private String buildFallbackResponse() {
        return "{\"intentType\":\"query\",\"metrics\":[],\"dimensions\":[],\"filters\":[],\"timeRange\":null,\"comparison\":null,\"needsClarification\":true,\"clarificationQuestions\":[\"AI服务暂时不可用\"]}";
    }

    private String cleanJson(String json) {
        String c = json.trim();
        if (c.startsWith("```json")) c = c.substring(7); else if (c.startsWith("```")) c = c.substring(3);
        if (c.endsWith("```")) c = c.substring(0, c.length() - 3);
        return c.trim();
    }
}
