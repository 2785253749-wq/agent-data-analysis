package com.agent.service;

import com.agent.entity.AiModelEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek API client — reads active model config from AiModelService.
 * Uses the globally-default enabled model's base URL, model name, and whitelisted key ref.
 */
@Service
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final AiModelService modelService;
    private final RestClient restClient;

    public DeepSeekClient(AiModelService modelService) {
        this.modelService = modelService;
        AiModelEntity m = modelService.activeDefault();
        String apiKey = resolveApiKey(m.getApiKeyRef());
        this.restClient = RestClient.builder()
                .baseUrl(m.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        log.info("DeepSeekClient initialized (model={}, baseUrl={}, keyConfigured={})",
                m.getModelName(), m.getBaseUrl(), apiKey != null && !apiKey.isBlank());
    }

    /**
     * Send a chat completion request to DeepSeek.
     *
     * @param systemPrompt system message
     * @param userMessage  user message
     * @param temperature  sampling temperature (0.0 = deterministic)
     * @return the assistant's text response
     */
    @SuppressWarnings("unchecked")
    public String chat(String systemPrompt, String userMessage, double temperature) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        );

        String modelName = modelService.activeDefault().getModelName();
        Map<String, Object> body = Map.of(
                "model", modelName,
                "messages", messages,
                "temperature", temperature
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) throw new RuntimeException("Empty response from DeepSeek");

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) throw new RuntimeException("No choices in response");

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            log.debug("DeepSeek response ({} tokens): {}",
                    ((Map<String, Object>) response.get("usage")).get("total_tokens"),
                    content != null ? content.substring(0, Math.min(100, content.length())) : "null");

            return content;
        } catch (Exception e) {
            log.error("DeepSeek API call failed: {}", e.getMessage());
            throw new RuntimeException("DeepSeek call failed: " + e.getMessage(), e);
        }
    }

    private String resolveApiKey(String keyRef) {
        // Only whitelist env-var references reach here (AiModelService validates).
        String key = System.getProperty(keyRef);
        if (key == null || key.isBlank()) {
            key = System.getenv(keyRef);
        }
        // Allow construction in test env without key — calls will fail gracefully
        return key == null ? "" : key;
    }
}
