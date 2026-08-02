package com.agent.service;

import com.agent.dto.AuditLogDTO;
import com.agent.entity.AuditLogEntity;
import com.agent.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Writes append-only audit entries.
 * Constraint 2: save uses REQUIRES_NEW so FAILED logs survive a business-transaction rollback,
 * and audit-write failure never masks the original business exception.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository repo;
    private final AuditDetailSanitizer sanitizer;

    public AuditLogService(AuditLogRepository repo, AuditDetailSanitizer sanitizer) {
        this.repo = repo;
        this.sanitizer = sanitizer;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String operator, Long userId, String action, String resourceType,
                       Long resourceId, String result, String ipAddress,
                       Map<String, Object> detail) {
        AuditLogEntity e = new AuditLogEntity();
        e.setOperatorName(operator);
        e.setUserId(userId);
        e.setAction(action);
        e.setResourceType(resourceType);
        e.setResourceId(resourceId);
        e.setResult(result);
        e.setIpAddress(ipAddress);
        e.setDetail(sanitizer.safeDetail(detail));
        repo.save(e);
    }

    public Page<AuditLogDTO> search(String operator, String action,
                                    LocalDateTime start, LocalDateTime end,
                                    int page, int size) {
        return repo.search(blankToNull(operator), blankToNull(action), start, end,
                        PageRequest.of(page, size))
                .map(this::toDTO);
    }

    private AuditLogDTO toDTO(AuditLogEntity e) {
        return new AuditLogDTO(
                e.getId(), e.getOperatorName(), e.getUserId(), e.getAction(),
                e.getResourceType(), e.getResourceId(), e.getResult(),
                e.getDetail(), e.getIpAddress(), e.getCreatedAt());
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
