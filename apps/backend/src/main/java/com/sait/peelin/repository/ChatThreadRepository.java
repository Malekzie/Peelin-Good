package com.sait.peelin.repository;

import com.sait.peelin.model.ChatThread;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatThreadRepository extends JpaRepository<ChatThread, Integer> {

    @EntityGraph(attributePaths = {"customerUser", "employeeUser"})
    List<ChatThread> findByStatusOrderByUpdatedAtDesc(String status);

    @EntityGraph(attributePaths = {"customerUser", "employeeUser"})
    Optional<ChatThread> findById(Integer id);

    @EntityGraph(attributePaths = {"customerUser", "employeeUser"})
    Optional<ChatThread> findFirstByCustomerUser_UserIdAndStatusOrderByUpdatedAtDesc(UUID customerUserId, String status);

    @Query(value = """
            SELECT ct.thread_id
            FROM chat_thread ct
            WHERE ct.customer_user_id = :customerUserId
              AND ct.status = 'open'
            ORDER BY ct.updated_at DESC
            FETCH FIRST 1 ROWS ONLY
            """, nativeQuery = true)
    Integer findLatestOpenThreadIdByCustomerUserId(@Param("customerUserId") UUID customerUserId);
}
