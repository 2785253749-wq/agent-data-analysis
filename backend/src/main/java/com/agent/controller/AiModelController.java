package com.agent.controller;

import com.agent.dto.AiModelDTO;
import com.agent.dto.AiModelRequest;
import com.agent.service.AiModelService;
import com.agent.service.UserAccessContext;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI model config. Writes are admin-only; enabled list is readable by all (metadata only,
 * api_key_ref never exposed).
 */
@RestController
@RequestMapping("/api/admin/models")
public class AiModelController {

    private final AiModelService service;
    private final UserAccessContext access;

    public AiModelController(AiModelService service, UserAccessContext access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    public ResponseEntity<Page<AiModelDTO>> list(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.list(page, size));
    }

    @GetMapping("/active")
    public ResponseEntity<List<AiModelDTO>> active() {
        return ResponseEntity.ok(service.active());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AiModelDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    public ResponseEntity<AiModelDTO> create(@Valid @RequestBody AiModelRequest req) {
        requireAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AiModelDTO> update(@PathVariable Long id, @Valid @RequestBody AiModelRequest req) {
        requireAdmin();
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        requireAdmin();
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/set-default")
    public ResponseEntity<AiModelDTO> setDefault(@PathVariable Long id) {
        requireAdmin();
        return ResponseEntity.ok(service.setDefault(id));
    }

    private void requireAdmin() {
        if (!access.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("仅管理员可操作模型配置");
        }
    }
}
