package com.agent.controller;

import com.agent.service.DatasetAccessService;
import com.agent.service.UserAccessContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Dataset authorization — ADMIN only.
 */
@RestController
@RequestMapping("/api/admin/datasets/{datasetId}/access")
public class DatasetAccessController {

    private final DatasetAccessService service;
    private final UserAccessContext access;

    public DatasetAccessController(DatasetAccessService service, UserAccessContext access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    public ResponseEntity<List<Long>> authorizedUsers(@PathVariable Long datasetId) {
        requireAdmin();
        return ResponseEntity.ok(service.authorizedDatasetIds(datasetId)); // userIds with access
    }

    @PostMapping
    public ResponseEntity<Void> grant(@PathVariable Long datasetId,
                                      @RequestParam Long userId) {
        requireAdmin();
        service.grant(userId, datasetId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> revoke(@PathVariable Long datasetId,
                                       @PathVariable Long userId) {
        requireAdmin();
        service.revoke(userId, datasetId);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin() {
        if (!access.isAdmin()) {
            throw new AccessDeniedException("仅管理员可授权数据集");
        }
    }
}
