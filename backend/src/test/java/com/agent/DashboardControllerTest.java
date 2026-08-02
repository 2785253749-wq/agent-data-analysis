package com.agent;

import com.agent.entity.AnalysisTaskEntity;
import com.agent.entity.DatasetEntity;
import com.agent.repository.AnalysisTaskRepository;
import com.agent.repository.DatasetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("DashboardController")
class DashboardControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DatasetRepository datasetRepo;
    @Autowired private AnalysisTaskRepository taskRepo;

    @BeforeEach
    void seed() {
        DatasetEntity ds = new DatasetEntity();
        ds.setName("销售数据"); ds.setTableName("sales"); ds.setOrgId(0L); ds.setIsEnabled(true);
        datasetRepo.save(ds);

        // 2 COMPLETED + 1 FAILED → success rate = 2/3 = 66.67
        taskRepo.save(task("完成1", "COMPLETED", LocalDateTime.now().minusHours(2)));
        taskRepo.save(task("完成2", "COMPLETED", LocalDateTime.now().minusHours(1)));
        taskRepo.save(task("失败1", "FAILED", LocalDateTime.now()));
    }

    private AnalysisTaskEntity task(String q, String status, LocalDateTime created) {
        AnalysisTaskEntity t = new AnalysisTaskEntity();
        t.setUserId(0L);
        t.setQuestion(q);
        t.setDatasetId(1L);
        t.setStatus(status);
        t.setStartedAt(created);
        t.setCompletedAt(created.plusSeconds(5));
        return t;
    }

    @Test
    @DisplayName("returns dashboard summary with counts and success rate")
    void summaryStructure() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary").with(httpBasic("admin", "test123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetCount").value(1))
                .andExpect(jsonPath("$.analysisCount").value(3))
                .andExpect(jsonPath("$.successRate").value(66.67))
                .andExpect(jsonPath("$.last7DaysTrend.length()").value(7))
                .andExpect(jsonPath("$.recentTasks.length()").value(3))
                .andExpect(jsonPath("$.commonFailures").isArray());
    }

    @Test
    @DisplayName("empty DB returns zero values with 7-day trend")
    void emptySummary() throws Exception {
        taskRepo.deleteAll();
        datasetRepo.deleteAll();

        mockMvc.perform(get("/api/dashboard/summary").with(httpBasic("admin", "test123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetCount").value(0))
                .andExpect(jsonPath("$.analysisCount").value(0))
                .andExpect(jsonPath("$.successRate").doesNotExist())
                .andExpect(jsonPath("$.last7DaysTrend.length()").value(7))
                .andExpect(jsonPath("$.recentTasks.length()").value(0));
    }
}
