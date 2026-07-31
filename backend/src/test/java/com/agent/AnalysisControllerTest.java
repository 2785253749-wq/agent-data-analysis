package com.agent;

import com.agent.dto.*;
import com.agent.repository.AnalysisTaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AnalysisController")
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AnalysisTaskRepository taskRepo;

    @Nested
    @DisplayName("POST /api/analysis/tasks")
    class CreateTask {

        @Test
        @DisplayName("should create analysis task with empty dataset and handle gracefully")
        void shouldCreateTaskAndHandleGracefully() throws Exception {
            String json = objectMapper.writeValueAsString(
                    new AnalysisRequest("测试问题", null));

            // The analysis will likely fail because DeepSeek is not available in test,
            // but the endpoint should still return a valid JSON response
            mockMvc.perform(post("/api/analysis/tasks")
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.taskId").exists())
                    .andExpect(jsonPath("$.question").value("测试问题"));
        }

        @Test
        @DisplayName("should return 400 when question is blank")
        void shouldRejectBlankQuestion() throws Exception {
            String json = objectMapper.writeValueAsString(
                    new AnalysisRequest("", 1L));

            mockMvc.perform(post("/api/analysis/tasks")
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should require authentication")
        void shouldRequireAuth() throws Exception {
            String json = objectMapper.writeValueAsString(
                    new AnalysisRequest("test", null));

            mockMvc.perform(post("/api/analysis/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("AnalysisResponse structure")
    class ResponseStructure {

        @Test
        @DisplayName("should have all required fields in response")
        void shouldHaveAllFields() {
            var response = new AnalysisResponse(
                    1L, "question", "COMPLETED",
                    null, null, null, null, null, null,
                    null,
                    List.of(new AnalysisResponse.StepInfo("INTENT", "COMPLETED", 100L)),
                    java.time.LocalDateTime.now(), java.time.LocalDateTime.now());

            assertEquals(1L, response.taskId());
            assertEquals("COMPLETED", response.status());
            assertEquals(1, response.steps().size());
            assertEquals("INTENT", response.steps().get(0).stepType());
        }
    }

    @Nested
    @DisplayName("Task persistence")
    class TaskPersistence {

        @Test
        @DisplayName("should persist task to database when analysis runs")
        void shouldPersistTask() throws Exception {
            String json = objectMapper.writeValueAsString(
                    new AnalysisRequest("持久化测试", null));

            String respBody = mockMvc.perform(post("/api/analysis/tasks")
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            var node = objectMapper.readTree(respBody);
            Long taskId = node.get("taskId").asLong();

            // Verify task exists in DB
            assertTrue(taskRepo.findById(taskId).isPresent());
            assertEquals("持久化测试", taskRepo.findById(taskId).get().getQuestion());
        }
    }
}
