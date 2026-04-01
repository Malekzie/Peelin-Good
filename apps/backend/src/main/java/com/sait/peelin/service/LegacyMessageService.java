package com.sait.peelin.service;

import com.sait.peelin.dto.v1.LegacyMessageDto;
import com.sait.peelin.dto.v1.SendLegacyMessageRequest;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.Message;
import com.sait.peelin.model.User;
import com.sait.peelin.repository.MessageRepository;
import com.sait.peelin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LegacyMessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<LegacyMessageDto> myMessages() {
        User u = currentUserService.requireUser();
        return messageRepository.findInvolvingUser(u.getUserId()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<LegacyMessageDto> conversation(UUID otherUserId) {
        User u = currentUserService.requireUser();
        return messageRepository.findConversation(u.getUserId(), otherUserId).stream().map(this::toDto).toList();
    }

    @Transactional
    public LegacyMessageDto send(SendLegacyMessageRequest req) {
        User sender = currentUserService.requireUser();
        User receiver = userRepository.findById(req.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));
        Message m = new Message();
        m.setSender(sender);
        m.setReceiver(receiver);
        m.setMessageSubject(req.getSubject());
        m.setMessageContent(req.getContent());
        m.setMessageIsRead(false);
        return toDto(messageRepository.save(m));
    }

    @Transactional
    public void markRead(UUID messageId) {
        Message m = messageRepository.findById(messageId).orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        m.setMessageIsRead(true);
        messageRepository.save(m);
    }

    private LegacyMessageDto toDto(Message m) {
        return new LegacyMessageDto(
                m.getId(),
                m.getSender().getUserId(),
                m.getReceiver().getUserId(),
                m.getMessageSubject(),
                m.getMessageContent(),
                m.getMessageSentDatetime(),
                Boolean.TRUE.equals(m.getMessageIsRead())
        );
    }
}
