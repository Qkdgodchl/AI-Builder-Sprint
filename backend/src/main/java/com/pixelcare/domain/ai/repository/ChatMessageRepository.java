package com.pixelcare.domain.ai.repository;

import com.pixelcare.domain.ai.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByUserIdAndIsDeletedFalseOrderByCreatedAtAsc(Long userId);
}
