package com.sait.peelin.repository;

import com.sait.peelin.model.ChatThread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatThreadRepository extends JpaRepository<ChatThread, Integer> {

    List<ChatThread> findByStatusOrderByUpdatedAtDesc(String status);

    Optional<ChatThread> findFirstByCustomerUser_UserIdAndStatusOrderByUpdatedAtDesc(UUID customerUserId, String status);
}
