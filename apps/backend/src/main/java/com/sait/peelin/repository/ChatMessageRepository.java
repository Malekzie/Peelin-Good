package com.sait.peelin.repository;

import com.sait.peelin.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    List<ChatMessage> findByThread_IdOrderBySentAtAsc(Integer threadId);
}
