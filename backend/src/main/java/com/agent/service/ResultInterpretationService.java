package com.agent.service;

import com.agent.dto.InterpretationDTO;
import com.agent.dto.QueryResult;
import com.agent.entity.PromptTemplateEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ResultInterpretationService {

    private static final Logger log = LoggerFactory.getLogger(ResultInterpretationService.class);
    private final DeepSeekClient deepSeek;
    private final PromptTemplateService promptService;
    private final ObjectMapper objectMapper;

    public ResultInterpretationService(DeepSeekClient deepSeek, PromptTemplateService promptService,
                                       ObjectMapper objectMapper) {
        this.deepSeek = deepSeek;
        this.promptService = promptService;
        this.objectMapper = objectMapper;
    }

    public InterpretationDTO interpret(String question, QueryResult result) {
        String context = buildContext(question, result);
        String jsonResponse = callModel(context);
        return parseInterpretation(jsonResponse);
    }

    public InterpretationDTO parseInterpretation(String json) {
        try {
            String cleaned = cleanJson(json);
            return objectMapper.readValue(cleaned, InterpretationDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse interpretation JSON", e);
            throw new IllegalArgumentException("Failed to parse model response as InterpretationDTO", e);
        }
    }

    private String callModel(String userMessage) {
        try {
            PromptTemplateEntity prompt = promptService.activeEntity(PromptTemplateEntity.TYPE_INTERPRET);
            return deepSeek.chat(prompt.getContent(), userMessage, 0.2);
        } catch (Exception e) {
            log.warn("DeepSeek interpretation failed: {}", e.getMessage());
            return "{\"conclusion\":\"AI服务暂时不可用\",\"points\":[],\"dataSufficient\":false,\"confidence\":\"low\",\"caveats\":[\"AI服务不可用\"]}";
        }
    }

    private String buildContext(String question, QueryResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户问题：").append(question).append("\n\n");
        sb.append("查询结果：").append(result.summary()).append("\n");
        sb.append("列名：").append(String.join(", ", result.columns())).append("\n");
        sb.append("行数：").append(result.rowCount()).append("\n");
        if (!result.rows().isEmpty()) {
            sb.append("数据预览（前3行）：\n");
            int count = 0;
            for (var row : result.rows()) { if (count++ >= 3) break; sb.append("  ").append(row).append("\n"); }
        }
        sb.append("\n请输出解释JSON：");
        return sb.toString();
    }

    private String cleanJson(String json) {
        String c = json.trim();
        if (c.startsWith("```json")) c = c.substring(7); else if (c.startsWith("```")) c = c.substring(3);
        if (c.endsWith("```")) c = c.substring(0, c.length() - 3);
        return c.trim();
    }
}
