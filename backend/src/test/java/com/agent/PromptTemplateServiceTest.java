package com.agent;

import com.agent.dto.PromptTemplateDTO;
import com.agent.entity.PromptTemplateEntity;
import com.agent.repository.PromptTemplateRepository;
import com.agent.service.PromptTemplateService;
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
@DisplayName("PromptTemplateService")
class PromptTemplateServiceTest {

    @Autowired private PromptTemplateService service;
    @Autowired private PromptTemplateRepository repo;

    private PromptTemplateDTO.CreateRequest req(String content) {
        return new PromptTemplateDTO.CreateRequest("sql-gen", PromptTemplateEntity.TYPE_SQL_GEN,
                content, null, "desc");
    }

    @BeforeEach
    void clean() {
        repo.deleteAll();
    }

    @Test
    @DisplayName("version auto-increments per type (constraint 5)")
    void versionIncrements() {
        var v1 = service.create(req("SELECT 1"));
        var v2 = service.create(req("SELECT 2"));
        assertEquals(1, v1.version());
        assertEquals(2, v2.version());
    }

    @Test
    @DisplayName("content is immutable — updateMeta only changes description (constraint 5)")
    void contentImmutable() {
        var created = service.create(req("SELECT 1"));
        var updated = service.updateMeta(created.id(), "new desc");
        assertEquals("SELECT 1", updated.content(), "content unchanged");
        assertEquals("new desc", updated.description());
    }

    @Test
    @DisplayName("contentHash is recorded (constraint 5)")
    void recordsContentHash() {
        var created = service.create(req("SELECT hashme"));
        assertEquals(64, created.contentHash().length(), "SHA-256 hex = 64 chars");
        assertFalse(created.contentHash().isEmpty());
    }

    @Test
    @DisplayName("one enabled version per type (constraint: unique enabled)")
    void oneEnabledPerType() {
        var v1 = service.create(req("SELECT 1"));
        var v2 = service.create(req("SELECT 2"));
        service.enable(v1.id());
        service.enable(v2.id());

        long enabled = repo.findByTypeAndIsEnabledTrue(PromptTemplateEntity.TYPE_SQL_GEN).size();
        assertEquals(1, enabled, "only one enabled");
    }

    @Test
    @DisplayName("cannot disable last enabled prompt of a required type (constraint 6)")
    void cannotDisableLastRequired() {
        var created = service.create(req("SELECT 1"));
        service.enable(created.id());
        assertThrows(IllegalArgumentException.class, () -> service.disable(created.id()));
    }

    @Test
    @DisplayName("new versions never auto-enable (constraint 5)")
    void newVersionNotAutoEnabled() {
        var v1 = service.create(req("SELECT 1"));
        service.enable(v1.id());
        var v2 = service.create(req("SELECT 2"));
        assertFalse(v2.isEnabled(), "new version not enabled by default");
    }
}
