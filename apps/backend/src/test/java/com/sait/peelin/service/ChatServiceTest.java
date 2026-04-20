package com.sait.peelin.service;

import com.sait.peelin.dto.v1.ChatThreadDto;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.ChatThread;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.ChatMessageRepository;
import com.sait.peelin.repository.ChatThreadRepository;
import com.sait.peelin.repository.EmployeeRepository;
import com.sait.peelin.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ChatThreadRepository chatThreadRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock ChatLookupCacheService chatLookupCacheService;
    @Mock CustomerLookupCacheService customerLookupCacheService;
    @Mock CurrentUserService currentUserService;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock ChatRoutingService chatRoutingService;
    @Mock UserRepository userRepository;
    @Mock EmployeeRepository employeeRepository;

    ChatService chatService;

    private User staffUser;
    private User customerUser;
    private ChatThread openThread;

    @BeforeEach
    void setUp() {
        UUID systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        chatService = new ChatService(
                chatThreadRepository, chatMessageRepository,
                chatLookupCacheService, customerLookupCacheService,
                currentUserService, messagingTemplate,
                chatRoutingService, userRepository, employeeRepository, systemUserId);

        staffUser = new User();
        staffUser.setUserId(UUID.randomUUID());
        staffUser.setUsername("staff1");
        staffUser.setUserRole(UserRole.employee);

        customerUser = new User();
        customerUser.setUserId(UUID.randomUUID());
        customerUser.setUsername("cust1");
        customerUser.setUserRole(UserRole.customer);

        openThread = new ChatThread();
        openThread.setId(42);
        openThread.setCustomerUser(customerUser);
        openThread.setStatus("open");
        openThread.setCategory("order_issue");
        openThread.setCreatedAt(OffsetDateTime.now());
        openThread.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    void createThread_WithCategory_PersistsCategory() {
        when(currentUserService.requireUser()).thenReturn(customerUser);
        when(chatThreadRepository.save(any(ChatThread.class))).thenAnswer(inv -> {
            ChatThread t = inv.getArgument(0);
            t.setId(1);
            return t;
        });
        when(currentUserService.currentUserOrNull()).thenReturn(customerUser);
        // No staff available — exercises the "no staff" branch without extra setup.
        when(chatRoutingService.pickStaff(any())).thenReturn(Optional.empty());
        // System user not found — postSystemMessage returns early (fail-quiet path).
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        ChatThreadDto result = chatService.createThread("order_issue");

        assertThat(result.category()).isEqualTo("order_issue");
        verify(chatLookupCacheService).evictOpenThreadForCustomer(customerUser.getUserId());
    }

    @Test
    void closeThread_ByStaff_SetsStatusAndClosedAt() {
        when(currentUserService.requireUser()).thenReturn(staffUser);
        when(chatThreadRepository.findById(42)).thenReturn(Optional.of(openThread));
        when(chatThreadRepository.save(any(ChatThread.class))).thenAnswer(inv -> inv.getArgument(0));
        when(currentUserService.currentUserOrNull()).thenReturn(staffUser);
        when(customerLookupCacheService.findByUserId(any())).thenReturn(null);

        ChatThreadDto result = chatService.closeThread(42);

        assertThat(result.status()).isEqualTo("closed");
        assertThat(result.closedAt()).isNotNull();
        verify(messagingTemplate).convertAndSendToUser(
                eq(customerUser.getUserId().toString()),
                eq("/queue/chat/notifications"),
                any()
        );
    }

    @Test
    @Disabled("Obsolete after close-thread role restriction was lifted; customers may now close their own threads.")
    void closeThread_ByCustomer_ThrowsForbidden() {
        when(currentUserService.requireUser()).thenReturn(customerUser);

        assertThatThrownBy(() -> chatService.closeThread(42))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void closeThread_ThreadNotFound_ThrowsNotFound() {
        when(currentUserService.requireUser()).thenReturn(staffUser);
        when(chatThreadRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.closeThread(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
