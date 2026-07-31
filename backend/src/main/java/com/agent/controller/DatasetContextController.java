package com.agent.controller;

import com.agent.dto.DatasetContextResponse;
import com.agent.service.DatasetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public metadata context endpoint for the Agent pipeline.
 *
 * GET /api/datasets/{id}/context
 *
 * Returns the dataset, all its fields, and all its metrics.
 * This is fed into DeepSeek prompts as the schema context for SQL generation.
 */
@RestController
@RequestMapping("/api/datasets")
public class DatasetContextController {

    private final DatasetService service;

    public DatasetContextController(DatasetService service) {
        this.service = service;
    }

    @GetMapping("/{id}/context")
    public ResponseEntity<DatasetContextResponse> getContext(@PathVariable Long id) {
        return ResponseEntity.ok(service.getContext(id));
    }
}
