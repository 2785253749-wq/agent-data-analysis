package com.agent.repository;

import com.agent.entity.AnalysisTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisTaskRepository extends JpaRepository<AnalysisTaskEntity, Long> {
    List<AnalysisTaskEntity> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
