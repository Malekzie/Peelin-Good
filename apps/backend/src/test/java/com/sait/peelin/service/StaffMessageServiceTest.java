package com.sait.peelin.service;

import com.sait.peelin.dto.v1.StaffConversationDto;
import com.sait.peelin.dto.v1.StaffMessageDto;
import com.sait.peelin.model.StaffConversation;
import com.sait.peelin.model.StaffMessage;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.StaffConversationRepository;
import com.sait.peelin.repository.StaffMessageRepository;
import com.sait.peelin.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffMessageServiceTest {

    @Mock StaffConversationRepository convRepo;
    @Mock StaffMessageRepository msgRepo;
    @Mock UserRepository userRepository;
    @Mock CurrentUserService currentUserService;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks StaffMessageService staffMessageService;

    private User userA;
    private User userB;
    private StaffConversation conversation;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        userA.setUsername("alice");
        userA.setUserRole(UserRole.employee);

        userB = new User();
        userB.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        userB.setUsername("bob");
        userB.setUserRole(UserRole.employee);

        conversation = new StaffConversation();
        conversation.setId(10);
        conversation.setUserA(userA);
        conversation.setUserB(userB);
        conversation.setCreatedAt(OffsetDateTime.now());
        conversation.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    void getOrCreateConversation_ExistingConversation_ReturnsSame() {
        when(currentUserService.requireUser()).thenReturn(userA);
        when(userRepository.findById(userB.getUserId())).thenReturn(Optional.of(userB));
        when(convRepo.findByUserA_UserIdAndUserB_UserId(userA.getUserId(), userB.getUserId()))
                .thenReturn(Optional.of(conversation));
        when(msgRepo.findByConversation_IdAndIsReadFalseAndSender_UserIdNot(10, userA.getUserId()))
                .thenReturn(Collections.emptyList());

        StaffConversationDto result = staffMessageService.getOrCreateConversation(userB.getUserId());

        assertThat(result.id()).isEqualTo(10);
        verify(convRepo, never()).save(any());
    }

    @Test
    void getOrCreateConversation_NewConversation_SavesThenReFetches() {
        when(currentUserService.requireUser()).thenReturn(userA);
        when(userRepository.findById(userB.getUserId())).thenReturn(Optional.of(userB));
        when(convRepo.findByUserA_UserIdAndUserB_UserId(userA.getUserId(), userB.getUserId()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(conversation));  // second call after save
        when(msgRepo.findByConversation_IdAndIsReadFalseAndSender_UserIdNot(10, userA.getUserId()))
                .thenReturn(Collections.emptyList());

        StaffConversationDto result = staffMessageService.getOrCreateConversation(userB.getUserId());

        assertThat(result.id()).isEqualTo(10);
        assertThat(result.otherUsername()).isEqualTo("bob");
        verify(convRepo).save(any(StaffConversation.class));
    }

    @Test
    void sendMessage_PersistsAndPushesViaWs() {
        StaffMessage saved = new StaffMessage();
        saved.setId(99);
        saved.setConversation(conversation);
        saved.setSender(userA);
        saved.setMessageText("hello");
        saved.setSentAt(OffsetDateTime.now());
        saved.setIsRead(false);

        when(currentUserService.requireUser()).thenReturn(userA);
        when(convRepo.findById(10)).thenReturn(Optional.of(conversation));
        when(msgRepo.save(any(StaffMessage.class))).thenReturn(saved);

        StaffMessageDto result = staffMessageService.sendMessage(10, "hello");

        assertThat(result.text()).isEqualTo("hello");
        verify(messagingTemplate).convertAndSend(
                eq("/topic/messages/conversation/10/messages"),
                any(StaffMessageDto.class));
        verify(messagingTemplate).convertAndSendToUser(
                eq(userB.getUserId().toString()),
                eq("/queue/messages/notifications"),
                any(StaffMessageDto.class));
    }

    @Test
    void sendMessage_NotParticipant_ThrowsForbidden() {
        User outsider = new User();
        outsider.setUserId(UUID.randomUUID());
        outsider.setUserRole(UserRole.employee);

        when(currentUserService.requireUser()).thenReturn(outsider);
        when(convRepo.findById(10)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> staffMessageService.sendMessage(10, "hi"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void conversations_ReturnsList() {
        when(currentUserService.requireUser()).thenReturn(userA);
        when(convRepo.findByUserA_UserIdOrUserB_UserIdOrderByUpdatedAtDesc(
                userA.getUserId(), userA.getUserId()))
                .thenReturn(List.of(conversation));
        when(msgRepo.findByConversation_IdAndIsReadFalseAndSender_UserIdNot(10, userA.getUserId()))
                .thenReturn(Collections.emptyList());

        List<StaffConversationDto> result = staffMessageService.conversations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10);
    }
}
