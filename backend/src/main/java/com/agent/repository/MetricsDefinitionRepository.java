package com.agent.repository;

import com.agent.entity.MetricsDefinitionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetricsDefinitionRepository extends JpaRepository<MetricsDefinitionEntity, Long> {

    Page<MetricsDefinitionEntity> findByDatasetId(Long datasetId, Pageable pageable);

    List<MetricsDefinitionEntity> findAllByDatasetId(Long datasetId);
}
