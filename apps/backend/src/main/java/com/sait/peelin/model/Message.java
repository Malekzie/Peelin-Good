package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "message")
public class Message {
    @Id
    @GeneratedValue
    @Column(name = "message_id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Size(max = 255)
    @NotNull
    @Column(name = "message_subject", nullable = false)
    private String messageSubject;

    @Size(max = 2000)
    @NotNull
    @Column(name = "message_content", nullable = false, length = 2000)
    private String messageContent;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "message_sent_datetime", nullable = false)
    private OffsetDateTime messageSentDatetime;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "message_is_read", nullable = false)
    private Boolean messageIsRead;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getMessageSubject() {
        return messageSubject;
    }

    public void setMessageSubject(String messageSubject) {
        this.messageSubject = messageSubject;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public OffsetDateTime getMessageSentDatetime() {
        return messageSentDatetime;
    }

    public void setMessageSentDatetime(OffsetDateTime messageSentDatetime) {
        this.messageSentDatetime = messageSentDatetime;
    }

    public Boolean getMessageIsRead() {
        return messageIsRead;
    }

    public void setMessageIsRead(Boolean messageIsRead) {
        this.messageIsRead = messageIsRead;
    }

}