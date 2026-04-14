package com.sait.peelin.repository;

import com.sait.peelin.model.StaffMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StaffMessageRepository extends JpaRepository<StaffMessage, Integer> {

    List<StaffMessage> findByConversation_IdOrderBySentAtAsc(Integer conversationId);

    List<StaffMessage> findByConversation_IdAndIsReadFalseAndSender_UserIdNot(
            Integer conversationId, UUID senderId);

    @Modifying
    @Query("UPDATE StaffMessage m SET m.isRead = true WHERE m.conversation.id = :convoId AND m.sender.userId <> :senderId AND m.isRead = false")
    int markAllReadForConversation(@Param("convoId") Integer convoId, @Param("senderId") UUID senderId);
}
