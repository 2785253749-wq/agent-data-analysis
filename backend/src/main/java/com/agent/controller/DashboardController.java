package com.agent.controller;

import com.agent.dto.DashboardSummaryDTO;
import com.agent.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only dashboard summary (admin + regular users, scoped by permission).
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> summary() {
        return ResponseEntity.ok(service.summary());
    }
}
