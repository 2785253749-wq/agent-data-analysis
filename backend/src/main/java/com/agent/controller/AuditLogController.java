package com.agent.controller;

import com.agent.dto.AuditLogDTO;
import com.agent.service.AuditLogService;
import com.agent.service.UserAccessContext;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Append-only audit log query — admin only. No write endpoints exist.
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService service;
    private final UserAccessContext access;

    public AuditLogController(AuditLogService service, UserAccessContext access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    public ResponseEntity<Page<AuditLogDTO>> list(
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!access.isAdmin()) {
            throw new AccessDeniedException("仅管理员可查看操作日志");
        }
        return ResponseEntity.ok(service.search(operator, action, start, end, page, size));
    }
}
