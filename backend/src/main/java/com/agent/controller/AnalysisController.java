package com.agent.controller;

import com.agent.dto.AnalysisRequest;
import com.agent.dto.AnalysisResponse;
import com.agent.service.AnalysisOrchestrator;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisOrchestrator orchestrator;

    public AnalysisController(AnalysisOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * POST /api/analysis/tasks
     * Synchronous analysis — returns the complete result.
     */
    @PostMapping("/tasks")
    public ResponseEntity<AnalysisResponse> createTask(@Valid @RequestBody AnalysisRequest request) {
        return ResponseEntity.ok(orchestrator.analyze(request));
    }

    /**
     * GET /api/analysis/tasks/stream
     * SSE streaming analysis — emits progress events.
     */
    @PostMapping(value = "/tasks/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTask(@Valid @RequestBody AnalysisRequest request) {
        return orchestrator.analyzeStream(request);
    }
}
