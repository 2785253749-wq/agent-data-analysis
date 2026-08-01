package com.agent.repository;

import com.agent.entity.AiModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiModelRepository extends JpaRepository<AiModelEntity, Long> {

    List<AiModelEntity> findByIsEnabledTrueOrderByIdAsc();

    Optional<AiModelEntity> findByIsEnabledTrueAndIsDefaultTrue();

    /** Global default: exactly one enabled+default model system-wide. */
    Optional<AiModelEntity> findFirstByIsDefaultTrue();
}
