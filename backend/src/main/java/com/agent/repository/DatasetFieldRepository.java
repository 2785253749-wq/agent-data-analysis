package com.agent.repository;

import com.agent.entity.DatasetFieldEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatasetFieldRepository extends JpaRepository<DatasetFieldEntity, Long> {

    Page<DatasetFieldEntity> findByDatasetId(Long datasetId, Pageable pageable);

    List<DatasetFieldEntity> findAllByDatasetId(Long datasetId);

    boolean existsByDatasetIdAndFieldName(Long datasetId, String fieldName);

    boolean existsByDatasetIdAndFieldNameAndIdNot(Long datasetId, String fieldName, Long id);
}
