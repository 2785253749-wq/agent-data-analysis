package com.agent.controller;

import com.agent.dto.PagedResponse;
import com.agent.dto.TaskDetailResponse;
import com.agent.dto.TaskSummaryDTO;
import com.agent.service.TaskHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Analysis history + Agent execution trace.
 * Both share the same list endpoint; the frontend differentiates by mode
 * (/history focuses on question/result, /trace on status/steps).
 *
 * GET /api/analysis/tasks?page=&size=&status=&datasetIds=&keyword= → paged summary list
 * GET /api/analysis/tasks/{id}                                    → detail (ACL: owner/admin)
 */
@RestController
@RequestMapping("/api/analysis/tasks")
public class TaskHistoryController {

    private final TaskHistoryService service;

    public TaskHistoryController(TaskHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<TaskSummaryDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) List<Long> datasetIds,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(service.list(page, size, status, datasetIds, keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDetailResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(service.detail(id));
    }
}
