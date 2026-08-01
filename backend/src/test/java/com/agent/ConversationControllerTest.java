package com.agent;

import com.agent.entity.AnalysisTaskEntity;
import com.agent.entity.ConversationEntity;
import com.agent.repository.AnalysisTaskRepository;
import com.agent.repository.ConversationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ConversationController")
class ConversationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ConversationRepository convRepo;
    @Autowired private AnalysisTaskRepository taskRepo;

    @BeforeEach
    void setUp() {
        // no seed needed — conversations created per test
    }

    private Long createConv(String title, Long datasetId) {
        ConversationEntity c = new ConversationEntity();
        c.setUserId(0L);
        c.setTitle(title);
        c.setDatasetId(datasetId);
        c.setStatus("ACTIVE");
        c.setTaskCount(0);
        return convRepo.save(c).getId();
    }

    @Nested
    @DisplayName("Conversation CRUD")
    class Crud {

        @Test
        @DisplayName("should create conversation")
        void shouldCreate() throws Exception {
            mockMvc.perform(post("/api/conversations")
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("title", "Q2分析", "datasetId", 1))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.title").value("Q2分析"))
                    .andExpect(jsonPath("$.taskCount").value(0));
        }

        @Test
        @DisplayName("should list active conversations")
        void shouldList() throws Exception {
            createConv("会话A", 1L);
            createConv("会话B", 1L);

            mockMvc.perform(get("/api/conversations")
                            .with(httpBasic("admin", "test123")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].title").exists());
        }

        @Test
        @DisplayName("should rename conversation")
        void shouldRename() throws Exception {
            Long id = createConv("旧名", 1L);
            mockMvc.perform(put("/api/conversations/{id}", id)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("title", "新名", "datasetId", 1))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("新名"));
        }

        @Test
        @DisplayName("should archive conversation")
        void shouldArchive() throws Exception {
            Long id = createConv("待归档", 1L);
            mockMvc.perform(delete("/api/conversations/{id}", id)
                            .with(httpBasic("admin", "test123")))
                    .andExpect(status().isNoContent());

            // No longer in ACTIVE list
            mockMvc.perform(get("/api/conversations")
                            .with(httpBasic("admin", "test123")))
                    .andExpect(jsonPath("$.content.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Dataset switch rejection (补充点2)")
    class DatasetSwitch {

        @Test
        @DisplayName("should reject dataset switch when tasks exist")
        void shouldRejectSwitch() throws Exception {
            Long id = createConv("有任务会话", 1L);
            // Add a task to the conversation
            AnalysisTaskEntity t = new AnalysisTaskEntity();
            t.setUserId(0L);
            t.setQuestion("q");
            t.setDatasetId(1L);
            t.setConversationId(id);
            t.setStatus("COMPLETED");
            taskRepo.save(t);
            convRepo.findById(id).ifPresent(c -> { c.setTaskCount(1); convRepo.save(c); });

            mockMvc.perform(put("/api/conversations/{id}", id)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("title", "x", "datasetId", 2))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("不能切换数据集")));
        }

        @Test
        @DisplayName("should allow dataset switch when no tasks")
        void shouldAllowSwitchWhenEmpty() throws Exception {
            Long id = createConv("空会话", 1L);
            mockMvc.perform(put("/api/conversations/{id}", id)
                            .with(httpBasic("admin", "test123"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("title", "x", "datasetId", 2))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.datasetId").value(2));
        }
    }

    @Nested
    @DisplayName("Isolation (补充: 他人不可见)")
    class Isolation {

        @Test
        @DisplayName("should 404 when non-owner accesses conversation")
        @org.springframework.security.test.context.support.WithMockUser(username = "alice")
        void should404ForNonOwner() throws Exception {
            Long id = createConv("他人会话", 1L);
            mockMvc.perform(get("/api/conversations/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }
}
