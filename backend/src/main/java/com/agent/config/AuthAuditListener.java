package com.agent.config;

import com.agent.service.AuditLogService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Records login / logout via Spring Security events.
 */
@Component
public class AuthAuditListener {

    private final AuditLogService auditLogService;

    public AuthAuditListener(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @EventListener
    public void onLogin(AuthenticationSuccessEvent event) {
        String name = event.getAuthentication().getName();
        auditLogService.record(name, 0L, "LOGIN", "AUTH", null, "SUCCESS", null, java.util.Map.of());
    }

    @EventListener
    public void onLogout(LogoutSuccessEvent event) {
        String name = event.getAuthentication() != null
                ? event.getAuthentication().getName() : "unknown";
        auditLogService.record(name, 0L, "LOGOUT", "AUTH", null, "SUCCESS", null, java.util.Map.of());
    }
}
