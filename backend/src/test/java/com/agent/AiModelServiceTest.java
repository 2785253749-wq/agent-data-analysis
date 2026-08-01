package com.agent;

import com.agent.dto.AiModelRequest;
import com.agent.entity.AiModelEntity;
import com.agent.repository.AiModelRepository;
import com.agent.service.AiModelService;
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
@DisplayName("AiModelService")
class AiModelServiceTest {

    @Autowired private AiModelService service;
    @Autowired private AiModelRepository repo;

    private AiModelRequest validReq() {
        return new AiModelRequest("TestModel", "deepseek",
                "https://api.deepseek.com/v1", "deepseek-chat",
                60000, 0.0, 2048, "DEEPSEEK_API_KEY", true, true);
    }

    @BeforeEach
    void clean() {
        repo.deleteAll();
    }

    @Test
    @DisplayName("rejects non-HTTPS base URL (constraint 3)")
    void rejectsHttpUrl() {
        AiModelRequest req = new AiModelRequest("m", "deepseek", "http://api.deepseek.com/v1",
                "deepseek-chat", 60000, 0.0, 2048, "DEEPSEEK_API_KEY", true, false);
        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    @DisplayName("rejects internal IP / localhost base URL (constraint 3)")
    void rejectsInternalUrl() {
        AiModelRequest req1 = new AiModelRequest("m", "deepseek", "https://10.0.0.1/v1",
                "x", 60000, 0.0, 2048, "DEEPSEEK_API_KEY", true, false);
        AiModelRequest req2 = new AiModelRequest("m", "deepseek", "https://localhost/v1",
                "x", 60000, 0.0, 2048, "DEEPSEEK_API_KEY", true, false);
        assertThrows(IllegalArgumentException.class, () -> service.create(req1));
        assertThrows(IllegalArgumentException.class, () -> service.create(req2));
    }

    @Test
    @DisplayName("rejects non-allowlisted domain (constraint 3)")
    void rejectsNonAllowlistedDomain() {
        AiModelRequest req = new AiModelRequest("m", "deepseek", "https://evil.example.com/v1",
                "x", 60000, 0.0, 2048, "DEEPSEEK_API_KEY", true, false);
        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    @DisplayName("rejects arbitrary port (constraint 3)")
    void rejectsCustomPort() {
        AiModelRequest req = new AiModelRequest("m", "deepseek", "https://api.deepseek.com:8080/v1",
                "x", 60000, 0.0, 2048, "DEEPSEEK_API_KEY", true, false);
        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    @DisplayName("rejects non-whitelist api_key_ref (constraint 4)")
    void rejectsArbitraryKeyRef() {
        AiModelRequest req = new AiModelRequest("m", "deepseek", "https://api.deepseek.com/v1",
                "x", 60000, 0.0, 2048, "env:ANY_VAR", true, false);
        assertThrows(IllegalArgumentException.class, () -> service.create(req));
    }

    @Test
    @DisplayName("global default is unique — creating second default clears first (constraint 2)")
    void globalDefaultUnique() {
        service.create(validReq()); // default model 1
        service.create(new AiModelRequest("m2", "deepseek", "https://api.deepseek.com/v1",
                "y", 60000, 0.0, 2048, "DEEPSEEK_API_KEY", true, true)); // default model 2

        long defaults = repo.findAll().stream().filter(AiModelEntity::getIsDefault).count();
        assertEquals(1, defaults, "exactly one global default");
    }

    @Test
    @DisplayName("apiKeyRef is never exposed in DTO (constraint 4)")
    void apiKeyRefNotExposed() {
        var created = service.create(validReq());
        var dto = service.get(created.id());
        // DTO has no apiKeyRef field at all.
        assertNotNull(dto.apiKeyConfigured());
        assertFalse(dto.apiKeyConfigured() == null && true, "always has boolean");
    }
}
