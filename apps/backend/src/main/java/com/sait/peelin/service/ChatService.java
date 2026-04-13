package com.sait.peelin.service;

import com.sait.peelin.dto.v1.ChatMessageDto;
import com.sait.peelin.dto.v1.ChatThreadDto;
import com.sait.peelin.dto.v1.PostChatMessageRequest;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.ChatMessage;
import com.sait.peelin.model.ChatThread;
import com.sait.peelin.model.Customer;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.ChatMessageRepository;
import com.sait.peelin.repository.ChatThreadRepository;
import com.sait.peelin.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final String STATUS_OPEN = "open";

    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CustomerRepository customerRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<ChatThreadDto> openThreads() {
        return chatThreadRepository.findByStatusOrderByUpdatedAtDesc(STATUS_OPEN)
                .stream()
                .map(this::threadDto)
                .toList();
    }

    @Transactional
    public ChatThreadDto getOrCreateOpenThreadForCustomer() {
        User user = currentUserService.requireUser();
        assertCustomer(user);

        return chatThreadRepository
                .findFirstByCustomerUser_UserIdAndStatusOrderByUpdatedAtDesc(user.getUserId(), STATUS_OPEN)
                .map(this::threadDto)
                .orElseGet(() -> threadDto(createThreadEntity(user)));
    }

    @Transactional
    public ChatThreadDto createThread() {
        User user = currentUserService.requireUser();
        assertCustomer(user);
        return threadDto(createThreadEntity(user));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> messages(Integer threadId) {
        ChatThread thread = requireThread(threadId);
        User user = currentUserService.requireUser();
        return messages(thread, user);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> messages(Integer threadId, User user) {
        ChatThread thread = requireThread(threadId);
        return messages(thread, user);
    }

    @Transactional
    public ChatMessageDto postMessage(Integer threadId, PostChatMessageRequest req) {
        ChatThread thread = requireThread(threadId);
        User sender = currentUserService.requireUser();
        return postMessage(thread, req.getText(), sender);
    }

    @Transactional
    public ChatMessageDto postMessage(Integer threadId, String rawText, User sender) {
        ChatThread thread = requireThread(threadId);
        return postMessage(thread, rawText, sender);
    }

    @Transactional
    public ChatThreadDto assignEmployee(Integer threadId) {
        User staff = currentUserService.requireUser();
        ChatThread thread = requireThread(threadId);
        return assignEmployee(thread, staff);
    }

    @Transactional
    public boolean markRead(Integer threadId) {
        User viewer = currentUserService.requireUser();
        ChatThread thread = requireThread(threadId);
        return markRead(thread, viewer);
    }

    @Transactional
    public boolean markRead(Integer threadId, User viewer) {
        ChatThread thread = requireThread(threadId);
        return markRead(thread, viewer);
    }

    @Transactional(readOnly = true)
    public void assertUserCanAccessThread(Integer threadId, User user) {
        ChatThread thread = requireThread(threadId);
        assertUserCanAccessThread(thread, user);
    }

    private List<ChatMessageDto> messages(ChatThread thread, User user) {
        assertUserCanAccessThread(thread, user);

        return chatMessageRepository.findByThread_IdOrderBySentAtAsc(thread.getId())
                .stream()
                .map(this::msgDto)
                .toList();
    }

    private ChatMessageDto postMessage(ChatThread thread, String rawText, User sender) {
        assertUserCanAccessThread(thread, sender);
        assertThreadOpen(thread);

        String text = normalizeAndValidateMessageText(rawText);

        if (isStaff(sender) && thread.getEmployeeUser() == null) {
            thread.setEmployeeUser(sender);
        }

        thread.setUpdatedAt(OffsetDateTime.now());
        chatThreadRepository.save(thread);

        ChatMessage message = new ChatMessage();
        message.setThread(thread);
        message.setSender(sender);
        message.setMessageText(text);
        message.setSentAt(OffsetDateTime.now());
        message.setIsRead(false);

        return msgDto(chatMessageRepository.save(message));
    }

    private ChatThreadDto assignEmployee(ChatThread thread, User staff) {
        assertStaff(staff);

        if (thread.getEmployeeUser() == null) {
            thread.setEmployeeUser(staff);
            thread.setUpdatedAt(OffsetDateTime.now());
        }

        return threadDto(chatThreadRepository.save(thread));
    }

    private boolean markRead(ChatThread thread, User viewer) {
        assertUserCanAccessThread(thread, viewer);

        List<ChatMessage> messages = chatMessageRepository.findByThread_IdOrderBySentAtAsc(thread.getId());
        List<ChatMessage> dirty = new ArrayList<>();

        for (ChatMessage message : messages) {
            if (!message.getSender().getUserId().equals(viewer.getUserId())
                    && !Boolean.TRUE.equals(message.getIsRead())) {
                message.setIsRead(true);
                dirty.add(message);
            }
        }

        if (dirty.isEmpty()) {
            return false;
        }

        chatMessageRepository.saveAll(dirty);
        thread.setUpdatedAt(OffsetDateTime.now());
        chatThreadRepository.save(thread);
        return true;
    }

    private ChatThread requireThread(Integer threadId) {
        return chatThreadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
    }

    private ChatThread createThreadEntity(User customer) {
        ChatThread thread = new ChatThread();
        thread.setCustomerUser(customer);
        thread.setStatus(STATUS_OPEN);
        thread.setCreatedAt(OffsetDateTime.now());
        thread.setUpdatedAt(OffsetDateTime.now());
        return chatThreadRepository.save(thread);
    }

    private void assertCustomer(User user) {
        if (user.getUserRole() != UserRole.customer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void assertStaff(User user) {
        if (!isStaff(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private boolean isStaff(User user) {
        return user.getUserRole() == UserRole.employee || user.getUserRole() == UserRole.admin;
    }

    private void assertThreadOpen(ChatThread thread) {
        if (!STATUS_OPEN.equalsIgnoreCase(thread.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Thread is not open");
        }
    }

    private String normalizeAndValidateMessageText(String rawText) {
        String text = rawText == null ? "" : rawText.trim();

        if (text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message text must not be blank");
        }

        if (text.length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message text exceeds max length of " + MAX_MESSAGE_LENGTH
            );
        }

        return text;
    }

    private void assertUserCanAccessThread(ChatThread thread, User user) {
        if (user.getUserRole() == UserRole.admin) {
            return;
        }

        if (user.getUserRole() == UserRole.customer) {
            if (!thread.getCustomerUser().getUserId().equals(user.getUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            return;
        }

        if (user.getUserRole() == UserRole.employee) {
            if (thread.getEmployeeUser() != null
                    && !thread.getEmployeeUser().getUserId().equals(user.getUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private ChatThreadDto threadDto(ChatThread thread) {
        User customerUser = thread.getCustomerUser();
        Customer customer = customerRepository.findByUser_UserId(customerUser.getUserId()).orElse(null);

        return new ChatThreadDto(
                thread.getId(),
                customerUser.getUserId(),
                resolveCustomerDisplayName(customer, customerUser),
                customerUser.getUsername(),
                resolveCustomerEmail(customer, customerUser),
                thread.getEmployeeUser() != null ? thread.getEmployeeUser().getUserId() : null,
                thread.getStatus(),
                thread.getCreatedAt(),
                thread.getUpdatedAt()
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
        if (customer != null
                && customer.getCustomerEmail() != null
                && !customer.getCustomerEmail().trim().isEmpty()) {
            return customer.getCustomerEmail();
        }
        return customerUser.getUserEmail();
    }

    private ChatMessageDto msgDto(ChatMessage message) {
        User sender = message.getSender();

        return new ChatMessageDto(
                message.getId(),
                message.getThread().getId(),
                sender.getUserId(),
                sender.getUsername(),
                sender.getUserRole() != null ? sender.getUserRole().name() : null,
                message.getMessageText(),
                message.getSentAt(),
                Boolean.TRUE.equals(message.getIsRead())
        );
    }
}