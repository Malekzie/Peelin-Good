package com.sait.peelin.service;

import com.sait.peelin.dto.v1.ChatMessageDto;
import com.sait.peelin.dto.v1.ChatThreadDto;
import com.sait.peelin.dto.v1.PostChatMessageRequest;
import com.sait.peelin.dto.v1.ReadReceiptPayload;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.ChatMessage;
import com.sait.peelin.model.ChatThread;
import com.sait.peelin.model.Customer;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.ChatMessageRepository;
import com.sait.peelin.repository.ChatThreadRepository;
import com.sait.peelin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatLookupCacheService chatLookupCacheService;
    private final CustomerLookupCacheService customerLookupCacheService;
    private final CurrentUserService currentUserService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoutingService chatRoutingService;
    private final UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Qualifier("systemUserId")
    private final java.util.UUID systemUserId;

    @Transactional(readOnly = true)
    public List<ChatThreadDto> archivedThreads(String category) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (category != null && !category.isBlank()) {
            return chatThreadRepository
                    .findByStatusAndCategoryOrderByUpdatedAtDesc("closed", category)
                    .stream().map(this::threadDto).toList();
        }
        return chatThreadRepository.findByStatusOrderByUpdatedAtDesc("closed")
                .stream().map(this::threadDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatThreadDto> openThreads(String category) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() == UserRole.admin || u.getUserRole() == UserRole.employee) {
            if (category != null && !category.isBlank()) {
                return chatThreadRepository
                        .findByStatusAndCategoryOrderByUpdatedAtDesc("open", category)
                        .stream().map(this::threadDto).toList();
            }
            return chatThreadRepository.findByStatusOrderByUpdatedAtDesc("open")
                    .stream().map(this::threadDto).toList();
        }
        if (u.getUserRole() == UserRole.customer) {
            return java.util.Optional.ofNullable(
                    chatLookupCacheService.findOpenThreadIdForCustomer(u.getUserId()))
                    .flatMap(chatThreadRepository::findById)
                    .map(this::threadDto)
                    .stream().toList();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    public List<ChatThreadDto> openThreads() {
        return openThreads(null);
    }

    @Transactional
    public ChatThreadDto getOrCreateOpenThreadForCustomer() {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.customer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return java.util.Optional.ofNullable(chatLookupCacheService.findOpenThreadIdForCustomer(u.getUserId()))
                .flatMap(chatThreadRepository::findById)
                .map(this::threadDto)
                .orElseGet(() -> {
                    ChatThread created = createThreadEntity(u, "general");
                    chatLookupCacheService.evictOpenThreadForCustomer(u.getUserId());
                    ChatThreadDto dto = threadDto(created);
                    messagingTemplate.convertAndSend("/topic/chat/threads", dto);
                    return dto;
                });
    }

    @Transactional
    @CacheEvict(value = "chat-open-threads", allEntries = true)
    public ChatThreadDto createThread() {
        return createThread("general");
    }

    @Transactional
    @CacheEvict(value = "chat-open-threads", allEntries = true)
    public ChatThreadDto createThread(String category) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.customer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        ChatThread created = createThreadEntity(u, category);

        // Auto-route: pick an active specialist (fallback to any active staff).
        Optional<User> picked = chatRoutingService.pickStaff(created.getCategory());
        if (picked.isPresent()) {
            created.setEmployeeUser(picked.get());
            created.setUpdatedAt(OffsetDateTime.now());
            created = chatThreadRepository.save(created);
            postSystemMessage(created, "Assigned to " + displayNameFor(picked.get()));
        } else {
            postSystemMessage(created, "No staff online right now, we'll respond as soon as possible.");
        }

        chatLookupCacheService.evictOpenThreadForCustomer(u.getUserId());
        ChatThreadDto dto = threadDto(created);
        messagingTemplate.convertAndSend("/topic/chat/threads", dto);
        return dto;
    }

    private ChatThread createThreadEntity(User customer, String category) {
        ChatThread t = new ChatThread();
        t.setCustomerUser(customer);
        t.setStatus("open");
        t.setCategory(category != null ? category : "general");
        t.setCreatedAt(OffsetDateTime.now());
        t.setUpdatedAt(OffsetDateTime.now());
        return chatThreadRepository.save(t);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> messages(Integer threadId) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() == UserRole.customer) {
            return chatMessageRepository
                    .findByThread_IdAndThread_CustomerUser_UserIdOrderBySentAtAsc(threadId, u.getUserId())
                    .stream()
                    .map(this::msgDto)
                    .toList();
        }
        ChatThread t = chatThreadRepository.findById(threadId).orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
        assertCanAccessThread(t);
        return chatMessageRepository.findByThread_IdOrderBySentAtAsc(threadId).stream().map(this::msgDto).toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "chat-messages", key = "'thread:' + #threadId"),
            @CacheEvict(value = "chat-open-threads", allEntries = true)
    })
    public ChatMessageDto postMessage(Integer threadId, PostChatMessageRequest req) {
        ChatThread t = chatThreadRepository.findById(threadId).orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
        if ("closed".equals(t.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thread is closed");
        }
        assertCanAccessThread(t);
        User sender = currentUserService.requireUser();
        ChatMessage m = new ChatMessage();
        m.setThread(t);
        m.setSender(sender);
        m.setMessageText(req.getText());
        m.setSentAt(OffsetDateTime.now());
        m.setIsRead(false);
        t.setUpdatedAt(OffsetDateTime.now());
        chatThreadRepository.save(t);
        if (t.getCustomerUser() != null && t.getCustomerUser().getUserId() != null) {
            chatLookupCacheService.evictOpenThreadForCustomer(t.getCustomerUser().getUserId());
        }
        ChatMessageDto dto = msgDto(chatMessageRepository.save(m));
        messagingTemplate.convertAndSend("/topic/chat/thread/" + threadId + "/messages", dto);
        return dto;
    }

    @Transactional
    @CacheEvict(value = "chat-open-threads", allEntries = true)
    public ChatThreadDto assignEmployee(Integer threadId) {
        User staff = currentUserService.requireUser();
        if (staff.getUserRole() != UserRole.employee && staff.getUserRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        ChatThread t = chatThreadRepository.findById(threadId).orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
        if (t.getEmployeeUser() == null) {
            t.setEmployeeUser(staff);
            t.setUpdatedAt(OffsetDateTime.now());
        }
        if (t.getCustomerUser() != null && t.getCustomerUser().getUserId() != null) {
            chatLookupCacheService.evictOpenThreadForCustomer(t.getCustomerUser().getUserId());
        }
        return threadDto(chatThreadRepository.save(t));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "chat-open-threads", allEntries = true),
            @CacheEvict(value = "chat-messages", key = "'thread:' + #threadId")
    })
    public ChatThreadDto closeThread(Integer threadId) {
        User actor = currentUserService.requireUser();
        ChatThread t = chatThreadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
        if (actor.getUserRole() == UserRole.customer) {
            if (!t.getCustomerUser().getUserId().equals(actor.getUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        } else if (actor.getUserRole() != UserRole.employee && actor.getUserRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        t.setStatus("closed");
        t.setClosedAt(OffsetDateTime.now());
        t.setUpdatedAt(OffsetDateTime.now());
        if (t.getCustomerUser() != null) {
            chatLookupCacheService.evictOpenThreadForCustomer(t.getCustomerUser().getUserId());
        }
        ChatThreadDto dto = threadDto(chatThreadRepository.save(t));
        messagingTemplate.convertAndSendToUser(
                t.getCustomerUser().getUserId().toString(),
                "/queue/chat/notifications",
                dto);
        messagingTemplate.convertAndSend("/topic/chat/thread/" + threadId + "/status", dto);
        return dto;
    }

    @Transactional
    @CacheEvict(value = "chat-messages", key = "'thread:' + #threadId")
    public void markRead(Integer threadId) {
        chatThreadRepository.findById(threadId).orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
        User viewer = currentUserService.requireUser();
        chatMessageRepository.markAllReadForThread(threadId, viewer.getUserId());
        messagingTemplate.convertAndSend(
                "/topic/chat/thread/" + threadId + "/read",
                new ReadReceiptPayload(viewer.getUserId(), OffsetDateTime.now()));
    }

    private void assertCanAccessThread(ChatThread t) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() == UserRole.admin) return;
        if (u.getUserRole() == UserRole.customer) {
            if (!t.getCustomerUser().getUserId().equals(u.getUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            return;
        }
        if (u.getUserRole() == UserRole.employee) {
            if (t.getEmployeeUser() != null && !t.getEmployeeUser().getUserId().equals(u.getUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private ChatThreadDto threadDto(ChatThread t) {
        User actor = currentUserService.currentUserOrNull();
        User customerUser = t.getCustomerUser();
        // Customer hover/polling only needs thread identity; avoid extra customer-profile query in that path.
        Customer customer = (actor != null && actor.getUserRole() == UserRole.customer)
                ? null
                : customerLookupCacheService.findByUserId(customerUser.getUserId());
        return new ChatThreadDto(
                t.getId(),
                customerUser.getUserId(),
                resolveCustomerDisplayName(customer, customerUser),
                customerUser.getUsername(),
                resolveCustomerEmail(customer, customerUser),
                t.getEmployeeUser() != null ? t.getEmployeeUser().getUserId() : null,
                t.getStatus(),
                t.getCategory(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getClosedAt()
        );
    }

    private String resolveCustomerDisplayName(Customer customer, User customerUser) {
        if (customer != null) {
            String first = customer.getCustomerFirstName() != null ? customer.getCustomerFirstName().trim() : "";
            String last = customer.getCustomerLastName() != null ? customer.getCustomerLastName().trim() : "";
            String full = (first + " " + last).trim();
            if (!full.isEmpty()) {
                return full;
            }
        }
        String username = customerUser.getUsername();
        return username != null && !username.trim().isEmpty() ? username : null;
    }

    private String resolveCustomerEmail(Customer customer, User customerUser) {
        if (customer != null && customer.getCustomerEmail() != null && !customer.getCustomerEmail().trim().isEmpty()) {
            return customer.getCustomerEmail();
        }
        return customerUser.getUserEmail();
    }

    private ChatMessageDto msgDto(ChatMessage m) {
        return new ChatMessageDto(
                m.getId(),
                m.getThread().getId(),
                m.getSender().getUserId(),
                m.getMessageText(),
                m.getSentAt(),
                Boolean.TRUE.equals(m.getIsRead()),
                m.getSender() != null && systemUserId.equals(m.getSender().getUserId())
        );
    }

    private void postSystemMessage(ChatThread thread, String text) {
        User system = userRepository.findById(systemUserId).orElse(null);
        if (system == null) {
            return; // Migration not applied — fail quiet, thread still works.
        }
        ChatMessage m = new ChatMessage();
        m.setThread(thread);
        m.setSender(system);
        m.setMessageText(text);
        m.setSentAt(OffsetDateTime.now());
        m.setIsRead(false);
        ChatMessage saved = chatMessageRepository.save(m);
        ChatMessageDto dto = msgDto(saved);
        messagingTemplate.convertAndSend("/topic/chat/thread/" + thread.getId() + "/messages", dto);
    }

    private String displayNameFor(User u) {
        if (u == null) return "a staff member";
        String name = u.getUsername();
        return name != null && !name.isBlank() ? name : "a staff member";
    }
}
