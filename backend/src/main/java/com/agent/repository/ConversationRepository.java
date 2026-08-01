package com.agent.repository;

import com.agent.entity.ConversationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {

    Page<ConversationEntity> findByUserIdAndStatus(Long userId, String status, Pageable pageable);

    List<ConversationEntity> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, String status);
}
