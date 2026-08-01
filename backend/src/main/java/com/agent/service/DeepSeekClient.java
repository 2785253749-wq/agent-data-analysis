package com.agent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Minimal DeepSeek API client — replaces Spring AI's OpenAiApi which has URL bugs in M5.
 * Calls https://api.deepseek.com/v1/chat/completions directly.
 */
@Service
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);
    private static final String BASE_URL = "https://api.deepseek.com/v1";

    private final RestClient restClient;
    private final String apiKey;

    public DeepSeekClient() {
        this.apiKey = resolveApiKey();
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        log.info("DeepSeekClient initialized (baseUrl={}, apiKey={}...)", BASE_URL,
                apiKey != null && !apiKey.isBlank() ? apiKey.substring(0, Math.min(10, apiKey.length())) : "EMPTY");
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

        Map<String, Object> body = Map.of(
                "model", "deepseek-v4-pro",
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

    private String resolveApiKey() {
        String key = System.getProperty("DEEPSEEK_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getenv("DEEPSEEK_API_KEY");
        }
        // Allow construction in test env without key — calls will fail gracefully
        return key == null ? "" : key;
    }
}
