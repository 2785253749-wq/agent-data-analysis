package com.agent.aspect;

import com.agent.annotation.Audit;
import com.agent.service.AuditLogService;
import com.agent.service.UserAccessContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Audits @Audit-annotated SERVICE methods. One boundary per action (Service preferred).
 * Constraint 4-IP: only getRemoteAddr() is used unless a trusted reverse proxy is configured;
 * X-Forwarded-For is never trusted here.
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final UserAccessContext access;

    public AuditAspect(AuditLogService auditLogService, UserAccessContext access) {
        this.auditLogService = auditLogService;
        this.access = access;
    }

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        String operator = currentOperator();
        Long userId = access.currentUserId();
        String ip = remoteAddr();

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("method", joinPoint.getSignature().getName());
        detail.put("args", safeArgs(joinPoint.getArgs()));

        try {
            Object result = joinPoint.proceed();
            // Extract resource id from result if it's a record with id() accessor.
            Long resourceId = extractResourceId(result);
            auditLogService.record(operator, userId, audit.action(), audit.resourceType(),
                    resourceId, "SUCCESS", ip, detail);
            return result;
        } catch (Throwable t) {
            detail.put("error", String.valueOf(t.getMessage()).substring(0,
                    Math.min(200, String.valueOf(t.getMessage() == null ? "null" : t.getMessage()).length())));
            auditLogService.record(operator, userId, audit.action(), audit.resourceType(),
                    null, "FAILED", ip, detail);
            throw t;
        }
    }

    private String currentOperator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return "system";
    }

    private String remoteAddr() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        HttpServletRequest req = attrs.getRequest();
        return req.getRemoteAddr();
    }

    /** Never serialize full bodies — only arg counts + primitive name/id values. */
    private Object safeArgs(Object[] args) {
        if (args == null) return "[]";
        Map<String, Object> safe = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            if (a == null) { safe.put("arg" + i, null); continue; }
            if (a instanceof Number || a instanceof Boolean) { safe.put("arg" + i, a); continue; }
            // Skip request bodies / entity objects — record only type name.
            safe.put("arg" + i, a.getClass().getSimpleName());
        }
        return safe;
    }

    private Long extractResourceId(Object result) {
        if (result == null) return null;
        try {
            Object id = result.getClass().getMethod("id").invoke(result);
            return id instanceof Number n ? n.longValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
