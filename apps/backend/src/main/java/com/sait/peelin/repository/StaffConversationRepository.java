package com.sait.peelin.repository;

import com.sait.peelin.model.StaffConversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffConversationRepository extends JpaRepository<StaffConversation, Integer> {

    @EntityGraph(attributePaths = {"userA", "userB"})
    Optional<StaffConversation> findByUserA_UserIdAndUserB_UserId(UUID userAId, UUID userBId);

    @EntityGraph(attributePaths = {"userA", "userB"})
    List<StaffConversation> findByUserA_UserIdOrUserB_UserIdOrderByUpdatedAtDesc(
            UUID userAId, UUID userBId);
}
