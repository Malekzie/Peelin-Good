package com.sait.peelin.service;

import com.sait.peelin.dto.v1.ReadReceiptPayload;
import com.sait.peelin.dto.v1.StaffConversationDto;
import com.sait.peelin.dto.v1.StaffMessageDto;
import com.sait.peelin.dto.v1.StaffRecipientDto;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.StaffConversation;
import com.sait.peelin.model.StaffMessage;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.StaffConversationRepository;
import com.sait.peelin.repository.StaffMessageRepository;
import com.sait.peelin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffMessageService {

    private final StaffConversationRepository convRepo;
    private final StaffMessageRepository msgRepo;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public StaffConversationDto getOrCreateConversation(UUID recipientId) {
        User me = currentUserService.requireUser();
        requireStaff(me);

        if (me.getUserId().equals(recipientId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot message yourself");
        }

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (recipient.getUserRole() != UserRole.employee && recipient.getUserRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient must be a staff member");
        }

        UUID a = me.getUserId().compareTo(recipientId) < 0 ? me.getUserId() : recipientId;
        UUID b = me.getUserId().compareTo(recipientId) < 0 ? recipientId : me.getUserId();
        User ua = me.getUserId().equals(a) ? me : recipient;
        User ub = me.getUserId().equals(a) ? recipient : me;

        convRepo.findByUserA_UserIdAndUserB_UserId(a, b).ifPresentOrElse(
                c -> {},
                () -> {
                    StaffConversation c = new StaffConversation();
                    c.setUserA(ua);
                    c.setUserB(ub);
                    c.setCreatedAt(OffsetDateTime.now());
                    c.setUpdatedAt(OffsetDateTime.now());
                    convRepo.save(c);
                }
        );

        StaffConversation conv = convRepo.findByUserA_UserIdAndUserB_UserId(a, b)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        return conversationDto(conv, me.getUserId());
    }

    @Transactional(readOnly = true)
    public List<StaffConversationDto> conversations() {
        User me = currentUserService.requireUser();
        requireStaff(me);
        return convRepo.findByUserA_UserIdOrUserB_UserIdOrderByUpdatedAtDesc(
                me.getUserId(), me.getUserId())
                .stream().map(c -> conversationDto(c, me.getUserId())).toList();
    }

    @Transactional(readOnly = true)
    public List<StaffRecipientDto> listRecipients() {
        User me = currentUserService.requireUser();
        requireStaff(me);
        return userRepository.findAll().stream()
                .filter(u -> u.getUserRole() == UserRole.admin || u.getUserRole() == UserRole.employee)
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .filter(u -> !u.getUserId().equals(me.getUserId()))
                .map(u -> new StaffRecipientDto(
                        u.getUserId(),
                        u.getUsername(),
                        u.getUserRole().name()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StaffMessageDto> messages(Integer conversationId) {
        User me = currentUserService.requireUser();
        requireStaff(me);
        StaffConversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        requireParticipant(conv, me);
        return msgRepo.findByConversation_IdOrderBySentAtAsc(conversationId)
                .stream().map(this::msgDto).toList();
    }

    @Transactional
    public StaffMessageDto sendMessage(Integer conversationId, String text) {
        User me = currentUserService.requireUser();
        requireStaff(me);
        StaffConversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        requireParticipant(conv, me);

        StaffMessage m = new StaffMessage();
        m.setConversation(conv);
        m.setSender(me);
        m.setMessageText(text);
        m.setSentAt(OffsetDateTime.now());
        m.setIsRead(false);

        conv.setUpdatedAt(OffsetDateTime.now());
        convRepo.save(conv);

        StaffMessageDto dto = msgDto(msgRepo.save(m));
        messagingTemplate.convertAndSend(
                "/topic/messages/conversation/" + conversationId + "/messages", dto);
        User other = conv.getUserA().getUserId().equals(me.getUserId())
                ? conv.getUserB() : conv.getUserA();
        messagingTemplate.convertAndSendToUser(
                other.getUserId().toString(),
                "/queue/messages/notifications",
                dto);
        return dto;
    }

    @Transactional
    public void markRead(Integer conversationId) {
        User me = currentUserService.requireUser();
        requireStaff(me);
        StaffConversation conv = convRepo.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        requireParticipant(conv, me);

        msgRepo.markAllReadForConversation(conversationId, me.getUserId());
        messagingTemplate.convertAndSend(
                "/topic/messages/conversation/" + conversationId + "/read",
                new ReadReceiptPayload(me.getUserId(), OffsetDateTime.now()));
    }

    private StaffConversationDto conversationDto(StaffConversation conv, UUID myUserId) {
        User other = conv.getUserA().getUserId().equals(myUserId)
                ? conv.getUserB() : conv.getUserA();
        int unread = msgRepo.findByConversation_IdAndIsReadFalseAndSender_UserIdNot(
                conv.getId(), myUserId).size();
        return new StaffConversationDto(
                conv.getId(), other.getUserId(), other.getUsername(),
                conv.getUpdatedAt(), unread);
    }

    private StaffMessageDto msgDto(StaffMessage m) {
        return new StaffMessageDto(
                m.getId(), m.getConversation().getId(),
                m.getSender().getUserId(), m.getMessageText(),
                m.getSentAt(), m.getIsRead());
    }

    private void requireStaff(User u) {
        if (u.getUserRole() != UserRole.employee && u.getUserRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void requireParticipant(StaffConversation conv, User u) {
        boolean participant = conv.getUserA().getUserId().equals(u.getUserId())
                || conv.getUserB().getUserId().equals(u.getUserId());
        if (!participant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
