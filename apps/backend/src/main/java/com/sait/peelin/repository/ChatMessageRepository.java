package com.sait.peelin.repository;

import com.sait.peelin.model.ChatMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    @EntityGraph(attributePaths = {"sender"})
    List<ChatMessage> findByThread_IdOrderBySentAtAsc(Integer threadId);

    @EntityGraph(attributePaths = {"sender"})
    List<ChatMessage> findByThread_IdAndThread_CustomerUser_UserIdOrderBySentAtAsc(Integer threadId, java.util.UUID customerUserId);
}
