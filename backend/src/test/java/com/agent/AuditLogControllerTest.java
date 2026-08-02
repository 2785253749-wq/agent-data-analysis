package com.agent;

import com.agent.repository.AuditLogRepository;
import com.agent.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuditLogController")
class AuditLogControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuditLogRepository repo;
    @Autowired private AuditLogService service;

    @BeforeEach
    void seed() {
        repo.deleteAll();
        service.record("admin", 0L, "DATASET_CREATE", "DATASET", 1L, "SUCCESS", "127.0.0.1", Map.of("k", "v"));
        service.record("admin", 0L, "MODEL_CREATE", "MODEL", 2L, "SUCCESS", "127.0.0.1", Map.of("k", "v"));
    }

    @Test
    @DisplayName("admin can paginate audit logs")
    void adminListsLogs() throws Exception {
        // seed 2 + possible LOGIN event from httpBasic auth → assert at least 2
        mockMvc.perform(get("/api/admin/audit-logs").with(httpBasic("admin", "test123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("filter by action")
    void filterByAction() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs").with(httpBasic("admin", "test123"))
                        .param("action", "MODEL_CREATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].action").value("MODEL_CREATE"));
    }

    @Test
    @DisplayName("filter by operator")
    void filterByOperator() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs").with(httpBasic("admin", "test123"))
                        .param("operator", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("non-admin cannot query logs")
    @org.springframework.security.test.context.support.WithMockUser(username = "alice")
    void nonAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }
}
