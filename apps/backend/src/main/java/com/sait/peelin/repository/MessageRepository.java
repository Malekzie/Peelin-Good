package com.sait.peelin.repository;

import com.sait.peelin.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m WHERE m.sender.userId = :userId OR m.receiver.userId = :userId ORDER BY m.messageSentDatetime DESC")
    List<Message> findInvolvingUser(@Param("userId") UUID userId);

    @Query("SELECT m FROM Message m WHERE (m.sender.userId = :a AND m.receiver.userId = :b) OR (m.sender.userId = :b AND m.receiver.userId = :a) ORDER BY m.messageSentDatetime ASC")
    List<Message> findConversation(@Param("a") UUID a, @Param("b") UUID b);
}
