package com.agent.repository;

import com.agent.entity.AnalysisStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisStepRepository extends JpaRepository<AnalysisStepEntity, Long> {
    List<AnalysisStepEntity> findByTaskIdOrderByStepOrderAsc(Long taskId);
}
