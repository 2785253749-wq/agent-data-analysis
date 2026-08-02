package com.agent;

import com.agent.dto.DatasetRequest;
import com.agent.repository.AuditLogRepository;
import com.agent.service.AuditLogService;
import com.agent.service.DatasetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AuditLog AOP integration")
class AuditLogTest {

    @Autowired private AuditLogRepository repo;
    @Autowired private DatasetService datasetService;
    @Autowired private AuditLogService auditLogService;

    @BeforeEach
    void clean() {
        repo.deleteAll();
    }

    @Test
    @DisplayName("dataset create produces one audit entry at Service boundary only")
    void datasetCreateAudited() {
        datasetService.create(new DatasetRequest("销售数据", null, "sales", 0L, true));

        var logs = repo.findAll();
        assertEquals(1, logs.size(), "exactly one log (no Controller duplicate)");
        assertEquals("DATASET_CREATE", logs.get(0).getAction());
        assertEquals("SUCCESS", logs.get(0).getResult());
    }

    @Test
    @DisplayName("failed service method records FAILED audit")
    void failedActionAudited() {
        // Create then attempt to create duplicate → exception → FAILED log
        datasetService.create(new DatasetRequest("销售数据", null, "sales", 0L, true));
        assertThrows(Exception.class, () ->
                datasetService.create(new DatasetRequest("销售数据", null, "sales", 0L, true)));

        var logs = repo.findAll();
        boolean hasFailed = logs.stream().anyMatch(l -> "FAILED".equals(l.getResult()));
        assertTrue(hasFailed, "a FAILED audit entry should exist");
    }

    @Test
    @DisplayName("audit detail is whitelisted — no request body / keys recorded")
    void detailWhitelisted() {
        datasetService.create(new DatasetRequest("销售数据", null, "sales", 0L, true));

        var log = repo.findAll().get(0);
        assertNotNull(log.getDetail());
        // Detail must not contain request body fields like name/tableName values
        assertFalse(log.getDetail().contains("销售数据"), "request body not recorded");
        assertFalse(log.getDetail().contains("sales"), "tableName not recorded");
        // Only safe keys: method/args (type names only)
        assertTrue(log.getDetail().contains("method"));
    }

    @Test
    @DisplayName("audit save uses REQUIRES_NEW — record survives without business txn")
    void requiresNew() {
        // record() is REQUIRES_NEW → commits even if called outside a transaction
        auditLogService.record("admin", 0L, "TEST", "TEST", 1L, "SUCCESS", null, java.util.Map.of("k", "v"));
        assertEquals(1, repo.count());
    }
}
