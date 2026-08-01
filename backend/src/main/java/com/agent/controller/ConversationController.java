package com.agent.controller;

import com.agent.dto.*;
import com.agent.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Multi-turn conversation endpoints.
 *
 * POST   /api/conversations            → create
 * GET    /api/conversations?status=    → paged list
 * GET    /api/conversations/{id}       → detail (context + turns)
 * PUT    /api/conversations/{id}       → rename / dataset (rejected if tasks exist)
 * DELETE /api/conversations/{id}       → archive (soft delete)
 * POST   /api/conversations/{id}/messages → follow-up analysis (task-as-turn)
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService service;

    public ConversationController(ConversationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ConversationSummaryDTO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.list(status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDetailDTO> detail(@PathVariable Long id) {
        return ResponseEntity.ok(service.detail(id));
    }

    @PostMapping
    public ResponseEntity<ConversationSummaryDTO> create(@Valid @RequestBody ConversationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConversationSummaryDTO> update(@PathVariable Long id,
                                                          @Valid @RequestBody ConversationRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        service.archive(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<AnalysisResponse> message(@PathVariable Long id,
                                                     @Valid @RequestBody ConversationMessageRequest req) {
        return ResponseEntity.ok(service.message(id, req));
    }
}
