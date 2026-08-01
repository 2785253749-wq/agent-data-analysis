package com.agent.controller;

import com.agent.dto.PromptTemplateDTO;
import com.agent.service.PromptTemplateService;
import com.agent.service.UserAccessContext;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Prompt template management. Writes are admin-only.
 * Regular users may only read active template METADATA (name/version), never content.
 */
@RestController
@RequestMapping("/api/admin/prompts")
public class PromptTemplateController {

    private final PromptTemplateService service;
    private final UserAccessContext access;

    public PromptTemplateController(PromptTemplateService service, UserAccessContext access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    public ResponseEntity<Page<PromptTemplateDTO>> list(@RequestParam(required = false) String type,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.list(type, page, size));
    }

    /** Active template metadata for a type — safe for regular users (no content). */
    @GetMapping("/active")
    public ResponseEntity<PromptTemplateDTO> active(@RequestParam String type) {
        return ResponseEntity.ok(service.activeMeta(type));
    }

    @PostMapping
    public ResponseEntity<PromptTemplateDTO> create(@Valid @RequestBody PromptTemplateDTO.CreateRequest req) {
        requireAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromptTemplateDTO> updateMeta(@PathVariable Long id,
                                                        @RequestParam(required = false) String description) {
        requireAdmin();
        return ResponseEntity.ok(service.updateMeta(id, description));
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<PromptTemplateDTO> enable(@PathVariable Long id) {
        requireAdmin();
        return ResponseEntity.ok(service.enable(id));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<PromptTemplateDTO> disable(@PathVariable Long id) {
        requireAdmin();
        return ResponseEntity.ok(service.disable(id));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        requireAdmin();
        service.archive(id);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin() {
        if (!access.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("仅管理员可操作Prompt模板");
        }
    }
}
