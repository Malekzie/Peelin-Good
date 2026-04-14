package com.sait.peelin.repository;

import com.sait.peelin.model.ChatMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    @EntityGraph(attributePaths = {"sender"})
    List<ChatMessage> findByThread_IdOrderBySentAtAsc(Integer threadId);

    @EntityGraph(attributePaths = {"sender"})
    List<ChatMessage> findByThread_IdAndThread_CustomerUser_UserIdOrderBySentAtAsc(Integer threadId, UUID customerUserId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.thread.id = :threadId AND m.sender.userId <> :senderId AND m.isRead = false")
    int markAllReadForThread(@Param("threadId") Integer threadId, @Param("senderId") UUID senderId);
}
