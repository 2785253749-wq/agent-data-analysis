package com.agent.controller;

import com.agent.dto.UserDTO;
import com.agent.service.UserAccessContext;
import com.agent.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

/**
 * User management — ADMIN only. Never returns passwords.
 */
@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService service;
    private final UserAccessContext access;

    public UserController(UserService service, UserAccessContext access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping
    public ResponseEntity<Page<UserDTO>> list(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin();
        return ResponseEntity.ok(service.list(access.currentOrgId(), role, page, size));
    }

    @PostMapping
    public ResponseEntity<UserDTO> create(@Valid @RequestBody UserDTO.CreateRequest req) {
        requireAdmin();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(req, access.currentOrgId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> update(@PathVariable Long id,
                                           @Valid @RequestBody UserDTO.UpdateRequest req) {
        requireAdmin();
        return ResponseEntity.ok(service.update(id, req, access.currentOrgId(), access.currentUsername()));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id,
                                              @RequestParam String newPassword) {
        requireAdmin();
        service.resetPassword(id, newPassword, access.currentOrgId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        requireAdmin();
        service.delete(id, access.currentOrgId(), access.currentUsername());
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin() {
        if (!access.isAdmin()) {
            throw new AccessDeniedException("仅管理员可管理用户");
        }
    }
}
