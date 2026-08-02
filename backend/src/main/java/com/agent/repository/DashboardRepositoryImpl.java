package com.agent.repository;

import com.agent.entity.AnalysisTaskEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DB-side aggregation queries for the dashboard (EntityManager, native/JPQL).
 */
@Repository
public class DashboardRepositoryImpl implements DashboardRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public long countDatasets(Long orgId) {
        return em.createQuery(
                "SELECT COUNT(d) FROM DatasetEntity d WHERE d.orgId = :orgId AND d.isEnabled = true",
                Long.class)
                .setParameter("orgId", orgId)
                .getSingleResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> countByTerminalStatus(boolean allDatasets, List<Long> datasetIds) {
        return em.createQuery(
                "SELECT t.status, COUNT(t) FROM AnalysisTaskEntity t " +
                "WHERE t.status IN ('COMPLETED','FAILED','CANCELLED') " +
                "AND (:all = true OR t.datasetId IN :ids) GROUP BY t.status")
                .setParameter("all", allDatasets)
                .setParameter("ids", datasetIds)
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> trendSince(LocalDateTime since, boolean allDatasets, List<Long> datasetIds) {
        // Native date function for GROUP BY on date component.
        return em.createNativeQuery(
                "SELECT DATE(t.created_at) AS d, COUNT(*) FROM analysis_tasks t " +
                "WHERE t.created_at >= :since " +
                "AND (:all = true OR t.dataset_id IN (:ids)) " +
                "GROUP BY DATE(t.created_at)")
                .setParameter("since", since)
                .setParameter("all", allDatasets)
                .setParameter("ids", datasetIds)
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AnalysisTaskEntity> recentTasks(boolean allDatasets, List<Long> datasetIds, Pageable pageable) {
        return em.createQuery(
                "SELECT t FROM AnalysisTaskEntity t " +
                "WHERE :all = true OR t.datasetId IN :ids ORDER BY t.createdAt DESC")
                .setParameter("all", allDatasets)
                .setParameter("ids", datasetIds)
                .setMaxResults(pageable.getPageSize())
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> commonFailures(boolean allDatasets, List<Long> datasetIds, Pageable pageable) {
        return em.createQuery(
                "SELECT s.failureCategory, COUNT(s) FROM AnalysisStepEntity s " +
                "WHERE s.status = 'FAILED' AND s.failureCategory IS NOT NULL " +
                "AND s.taskId IN (SELECT t.id FROM AnalysisTaskEntity t " +
                "  WHERE :all = true OR t.datasetId IN :ids) " +
                "GROUP BY s.failureCategory ORDER BY COUNT(s) DESC")
                .setParameter("all", allDatasets)
                .setParameter("ids", datasetIds)
                .setMaxResults(pageable.getPageSize())
                .getResultList();
    }
}
