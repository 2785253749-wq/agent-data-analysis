package com.agent.repository;

import com.agent.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    @Query("""
        SELECT a FROM AuditLogEntity a
        WHERE (:operator IS NULL OR a.operatorName = :operator)
          AND (:action IS NULL OR a.action = :action)
          AND (:start IS NULL OR a.createdAt >= :start)
          AND (:end IS NULL OR a.createdAt <= :end)
        ORDER BY a.createdAt DESC
    """)
    Page<AuditLogEntity> search(
            @Param("operator") String operator,
            @Param("action") String action,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
}
