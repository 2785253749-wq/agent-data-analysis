package com.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AdminConfigController")
class AdminConfigControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // ConfigSeeder seeds default model + prompts on context start.
    }

    @Test
    @DisplayName("admin can list models")
    void adminListsModels() throws Exception {
        mockMvc.perform(get("/api/admin/models").with(httpBasic("admin", "test123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("non-admin write is forbidden (constraint: admin-only)")
    @org.springframework.security.test.context.support.WithMockUser(username = "alice")
    void nonAdminCannotCreateModel() throws Exception {
        mockMvc.perform(post("/api/admin/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "x", "provider", "deepseek",
                                "baseUrl", "https://api.deepseek.com/v1",
                                "modelName", "deepseek-chat", "timeoutMs", 60000,
                                "temperature", 0.0, "maxTokens", 2048,
                                "apiKeyRef", "DEEPSEEK_API_KEY", "isEnabled", true, "isDefault", false))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("model DTO never exposes api_key_ref")
    void modelDoesNotExposeKeyRef() throws Exception {
        mockMvc.perform(get("/api/admin/models").with(httpBasic("admin", "test123")))
                .andExpect(jsonPath("$.content[0].apiKeyRef").doesNotExist())
                .andExpect(jsonPath("$.content[0].apiKeyConfigured").exists());
    }

    @Test
    @DisplayName("active prompt metadata for users has no content (constraint 1)")
    void userActivePromptHasNoContent() throws Exception {
        mockMvc.perform(get("/api/admin/prompts/active").param("type", "INTENT_RECOGNITION")
                        .with(httpBasic("admin", "test123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").doesNotExist())
                .andExpect(jsonPath("$.version").exists());
    }

    @Test
    @DisplayName("non-admin cannot create prompt (constraint: admin-only)")
    @org.springframework.security.test.context.support.WithMockUser(username = "alice")
    void nonAdminCannotCreatePrompt() throws Exception {
        mockMvc.perform(post("/api/admin/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "p", "type", "INTENT_RECOGNITION",
                                "content", "system", "description", "d"))))
                .andExpect(status().isForbidden());
    }
}
