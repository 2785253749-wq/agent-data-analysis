package com.agent.repository;

import com.agent.entity.AnalysisTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisTaskRepository extends JpaRepository<AnalysisTaskEntity, Long> {
}
