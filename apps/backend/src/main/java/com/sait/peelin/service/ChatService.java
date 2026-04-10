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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CustomerLookupCacheService customerLookupCacheService;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<ChatThreadDto> openThreads() {
        User u = currentUserService.requireUser();
        if (u.getUserRole() == UserRole.admin || u.getUserRole() == UserRole.employee) {
            return chatThreadRepository.findByStatusOrderByUpdatedAtDesc("open").stream().map(this::threadDto).toList();
        }
        if (u.getUserRole() == UserRole.customer) {
            return chatThreadRepository
                    .findFirstByCustomerUser_UserIdAndStatusOrderByUpdatedAtDesc(u.getUserId(), "open")
                    .map(this::threadDto)
                    .stream()
                    .toList();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @Transactional
    public ChatThreadDto getOrCreateOpenThreadForCustomer() {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.customer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return chatThreadRepository
                .findFirstByCustomerUser_UserIdAndStatusOrderByUpdatedAtDesc(u.getUserId(), "open")
                .map(this::threadDto)
                .orElseGet(() -> threadDto(createThread(u)));
    }

    @Transactional
    public ChatThreadDto createThread() {
        User u = currentUserService.requireUser();
        if (u.getUserRole() != UserRole.customer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return threadDto(createThread(u));
    }

    private ChatThread createThread(User customer) {
        ChatThread t = new ChatThread();
        t.setCustomerUser(customer);
        t.setStatus("open");
        t.setCreatedAt(OffsetDateTime.now());
        t.setUpdatedAt(OffsetDateTime.now());
        return chatThreadRepository.save(t);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> messages(Integer threadId) {
        ChatThread t = chatThreadRepository.findById(threadId).orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
        assertCanAccessThread(t);
        return chatMessageRepository.findByThread_IdOrderBySentAtAsc(threadId).stream().map(this::msgDto).toList();
    }

    @Transactional
    public ChatMessageDto postMessage(Integer threadId, PostChatMessageRequest req) {
        ChatThread t = chatThreadRepository.findById(threadId).orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
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
        return msgDto(chatMessageRepository.save(m));
    }

    @Transactional
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
        return threadDto(chatThreadRepository.save(t));
    }

    @Transactional
    public void markRead(Integer threadId) {
        chatThreadRepository.findById(threadId).orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
        User viewer = currentUserService.requireUser();
        List<ChatMessage> msgs = chatMessageRepository.findByThread_IdOrderBySentAtAsc(threadId);
        for (ChatMessage m : msgs) {
            if (!m.getSender().getUserId().equals(viewer.getUserId())) {
                m.setIsRead(true);
                chatMessageRepository.save(m);
            }
        }
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
        User customerUser = t.getCustomerUser();
        Customer customer = customerLookupCacheService.findByUserId(customerUser.getUserId());
        return new ChatThreadDto(
                t.getId(),
                customerUser.getUserId(),
                resolveCustomerDisplayName(customer, customerUser),
                customerUser.getUsername(),
                resolveCustomerEmail(customer, customerUser),
                t.getEmployeeUser() != null ? t.getEmployeeUser().getUserId() : null,
                t.getStatus(),
                t.getCreatedAt(),
                t.getUpdatedAt()
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
                Boolean.TRUE.equals(m.getIsRead())
        );
    }
}
