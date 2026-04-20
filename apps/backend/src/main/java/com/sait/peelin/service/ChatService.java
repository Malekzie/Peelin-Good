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
import com.sait.peelin.model.Employee;
import com.sait.peelin.repository.ChatMessageRepository;
import com.sait.peelin.repository.ChatThreadRepository;
import com.sait.peelin.repository.EmployeeRepository;
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
    private final EmployeeRepository employeeRepository;

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
            java.util.UUID myId = u.getUserId();
            List<ChatThread> all = (category != null && !category.isBlank())
                    ? chatThreadRepository.findByStatusAndCategoryOrderByUpdatedAtDesc("open", category)
                    : chatThreadRepository.findByStatusOrderByUpdatedAtDesc("open");
            return all.stream()
                    .filter(t -> t.getEmployeeUser() == null
                            || t.getEmployeeUser().getUserId().equals(myId))
                    .map(this::threadDto)
                    .toList();
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
    public ChatThreadDto getOpenThreadForCustomer() {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.customer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return java.util.Optional.ofNullable(chatLookupCacheService.findOpenThreadIdForCustomer(u.getUserId()))
                .flatMap(chatThreadRepository::findById)
                .map(this::threadDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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
        created = applyAutoRouting(created);

        chatLookupCacheService.evictOpenThreadForCustomer(u.getUserId());
        ChatThreadDto dto = threadDto(created);
        messagingTemplate.convertAndSend(ChatTopics.NEW_THREADS, dto);
        return dto;
    }

    private ChatThread applyAutoRouting(ChatThread thread) {
        Optional<User> picked = chatRoutingService.pickStaff(thread.getCategory());
        if (picked.isPresent()) {
            thread.setEmployeeUser(picked.get());
            thread.setUpdatedAt(OffsetDateTime.now());
            thread = chatThreadRepository.save(thread);
            postSystemMessage(thread, ChatSystemMessages.assignedTo(displayNameFor(picked.get())));
        } else {
            postSystemMessage(thread, ChatSystemMessages.NO_STAFF_ONLINE);
        }
        return thread;
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
                    .findByThread_IdAndThread_CustomerUser_UserIdAndIsStaffOnlyFalseOrderBySentAtAsc(threadId, u.getUserId())
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
        User sender = currentUserService.requireUser();
        // Customers may only post to their own thread. Staff must be the current assignee —
        // unassigned threads must be claimed first to prevent stepping on another agent.
        if (sender.getUserRole() == UserRole.customer) {
            assertCanAccessThread(t);
        } else {
            assertIsCurrentAssignee(t);
        }
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
        messagingTemplate.convertAndSend(ChatTopics.messages(threadId), dto);
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
        boolean newlyAssigned = false;
        if (t.getEmployeeUser() == null) {
            t.setEmployeeUser(staff);
            t.setUpdatedAt(OffsetDateTime.now());
            newlyAssigned = true;
        }
        if (t.getCustomerUser() != null && t.getCustomerUser().getUserId() != null) {
            chatLookupCacheService.evictOpenThreadForCustomer(t.getCustomerUser().getUserId());
        }
        ChatThread saved = chatThreadRepository.save(t);
        if (newlyAssigned) {
            postSystemMessage(saved, ChatSystemMessages.assignedTo(displayNameFor(staff)));
        }
        ChatThreadDto dto = threadDto(saved);
        if (newlyAssigned) {
            messagingTemplate.convertAndSend(ChatTopics.status(threadId), dto);
        }
        return dto;
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
        } else if (actor.getUserRole() == UserRole.employee || actor.getUserRole() == UserRole.admin) {
            assertIsCurrentAssignee(t);
        } else {
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
        messagingTemplate.convertAndSend(ChatTopics.status(threadId), dto);
        return dto;
    }

    @Transactional
    @CacheEvict(value = "chat-messages", key = "'thread:' + #threadId")
    public void markRead(Integer threadId) {
        ChatThread t = chatThreadRepository.findById(threadId).orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
        assertCanAccessThread(t);
        User viewer = currentUserService.requireUser();
        chatMessageRepository.markAllReadForThread(threadId, viewer.getUserId());
        messagingTemplate.convertAndSend(
                ChatTopics.read(threadId),
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
        if (u.getUserRole() == UserRole.employee || u.getUserRole() == UserRole.admin) {
            // Admin read-only audit access to closed threads; otherwise must be assignee.
            if (u.getUserRole() == UserRole.admin
                    && "closed".equalsIgnoreCase(t.getStatus())) {
                return;
            }
            if (t.getEmployeeUser() != null
                    && t.getEmployeeUser().getUserId().equals(u.getUserId())) {
                return;
            }
            // Unassigned open threads remain claimable — allow read so the picker works,
            // but postMessage still requires a concrete assignee (handled separately).
            if (t.getEmployeeUser() == null && "open".equalsIgnoreCase(t.getStatus())) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private void assertIsCurrentAssignee(ChatThread t) {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.employee && u.getUserRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (t.getEmployeeUser() == null
                || !t.getEmployeeUser().getUserId().equals(u.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the current assignee");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "chat-open-threads", allEntries = true),
            @CacheEvict(value = "chat-messages", key = "'thread:' + #threadId")
    })
    public ChatThreadDto transferThread(Integer threadId, java.util.UUID targetUserId) {
        ChatThread t = chatThreadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
        if ("closed".equalsIgnoreCase(t.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thread is closed");
        }
        assertIsCurrentAssignee(t);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target staff not found"));
        if (target.getUserRole() != UserRole.employee && target.getUserRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target is not staff");
        }
        if (!Boolean.TRUE.equals(target.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target is inactive");
        }
        User from = t.getEmployeeUser();
        if (target.getUserId().equals(from.getUserId())) {
            return threadDto(t);
        }
        t.setEmployeeUser(target);
        t.setUpdatedAt(OffsetDateTime.now());
        ChatThread saved = chatThreadRepository.save(t);
        // Customer sees a friendly, identity-free notice.
        postSystemMessage(saved, ChatSystemMessages.CUSTOMER_TRANSFER_NOTICE, false);
        // Staff audit trail preserves names + actor for archived-view timeline.
        postSystemMessage(saved,
                ChatSystemMessages.transferredBetween(displayNameFor(from), displayNameFor(target)),
                true);
        if (saved.getCustomerUser() != null) {
            chatLookupCacheService.evictOpenThreadForCustomer(saved.getCustomerUser().getUserId());
        }
        ChatThreadDto dto = threadDto(saved);
        messagingTemplate.convertAndSend(ChatTopics.status(threadId), dto);
        messagingTemplate.convertAndSend(ChatTopics.NEW_THREADS, dto);
        return dto;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "chat-open-threads", allEntries = true),
            @CacheEvict(value = "chat-messages", key = "'thread:' + #threadId")
    })
    public ChatThreadDto reopenThread(Integer threadId) {
        User actor = currentUserService.requireUser();
        if (actor.getUserRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins may reopen");
        }
        ChatThread t = chatThreadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
        if (!"closed".equalsIgnoreCase(t.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thread is not closed");
        }
        User previousAssignee = t.getEmployeeUser();
        t.setStatus("open");
        t.setClosedAt(null);
        t.setEmployeeUser(actor);
        t.setUpdatedAt(OffsetDateTime.now());
        ChatThread saved = chatThreadRepository.save(t);
        postSystemMessage(saved,
                ChatSystemMessages.reopenedForAudit(
                        displayNameFor(actor),
                        previousAssignee != null ? displayNameFor(previousAssignee) : null),
                true);
        if (saved.getCustomerUser() != null) {
            chatLookupCacheService.evictOpenThreadForCustomer(saved.getCustomerUser().getUserId());
        }
        ChatThreadDto dto = threadDto(saved);
        messagingTemplate.convertAndSend(ChatTopics.status(threadId), dto);
        messagingTemplate.convertAndSend(ChatTopics.NEW_THREADS, dto);
        return dto;
    }

    private ChatThreadDto threadDto(ChatThread t) {
        User actor = currentUserService.currentUserOrNull();
        User customerUser = t.getCustomerUser();
        // Customer hover/polling only needs thread identity; avoid extra customer-profile query in that path.
        Customer customer = (actor != null && actor.getUserRole() == UserRole.customer)
                ? null
                : customerLookupCacheService.findByUserId(customerUser.getUserId());
        User employeeUser = t.getEmployeeUser();
        return new ChatThreadDto(
                t.getId(),
                customerUser.getUserId(),
                resolveCustomerDisplayName(customer, customerUser),
                customerUser.getUsername(),
                resolveCustomerEmail(customer, customerUser),
                customerUser.getProfilePhotoPath(),
                employeeUser != null ? employeeUser.getUserId() : null,
                resolveEmployeeDisplayName(employeeUser),
                employeeUser != null ? employeeUser.getUsername() : null,
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
                m.getSender() != null && systemUserId.equals(m.getSender().getUserId()),
                Boolean.TRUE.equals(m.getIsStaffOnly())
        );
    }

    private void postSystemMessage(ChatThread thread, String text) {
        postSystemMessage(thread, text, false);
    }

    private void postSystemMessage(ChatThread thread, String text, boolean staffOnly) {
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
        m.setIsStaffOnly(staffOnly);
        ChatMessage saved = chatMessageRepository.save(m);
        ChatMessageDto dto = msgDto(saved);
        // Staff-only goes to a dedicated topic that the customer client does not subscribe to.
        String topic = staffOnly
                ? ChatTopics.staffMessages(thread.getId())
                : ChatTopics.messages(thread.getId());
        messagingTemplate.convertAndSend(topic, dto);
    }

    private String resolveEmployeeDisplayName(User employeeUser) {
        if (employeeUser == null) return null;
        Optional<Employee> emp = employeeRepository.findByUser_UserId(employeeUser.getUserId());
        if (emp.isPresent()) {
            String first = emp.get().getEmployeeFirstName();
            String last = emp.get().getEmployeeLastName();
            StringBuilder sb = new StringBuilder();
            if (first != null && !first.isBlank()) sb.append(first.trim());
            if (last != null && !last.isBlank()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(last.trim());
            }
            if (sb.length() > 0) return sb.toString();
        }
        String username = employeeUser.getUsername();
        return username != null && !username.isBlank() ? username : null;
    }

    private String displayNameFor(User u) {
        if (u == null) return "a staff member";
        String name = u.getUsername();
        return name != null && !name.isBlank() ? name : "a staff member";
    }
}
