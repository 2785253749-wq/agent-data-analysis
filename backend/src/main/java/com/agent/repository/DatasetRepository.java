package com.agent.repository;

import com.agent.entity.DatasetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<DatasetEntity, Long> {

    Page<DatasetEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByOrgIdAndTableName(Long orgId, String tableName);

    boolean existsByOrgIdAndTableNameAndIdNot(Long orgId, String tableName, Long id);
}
