package com.agent.repository;

import com.agent.entity.PromptTemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplateEntity, Long> {

    Optional<PromptTemplateEntity> findFirstByTypeOrderByVersionDesc(String type);

    List<PromptTemplateEntity> findByTypeOrderByVersionDesc(String type);

    Page<PromptTemplateEntity> findByType(String type, Pageable pageable);

    List<PromptTemplateEntity> findByTypeAndIsEnabledTrue(String type);

    Optional<PromptTemplateEntity> findFirstByTypeAndIsEnabledTrue(String type);

    boolean existsByTypeAndVersion(String type, Integer version);

    Optional<PromptTemplateEntity> findFirstByTypeAndIsArchivedFalseOrderByVersionDesc(String type);
}
