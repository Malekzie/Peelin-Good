package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.ChatMessageDto;
import com.sait.peelin.dto.v1.ChatThreadDto;
import com.sait.peelin.dto.v1.PostChatMessageRequest;
import com.sait.peelin.repository.EmployeeSpecialtyRepository;
import com.sait.peelin.service.ChatService;
import com.sait.peelin.service.CurrentUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Support chat between customers and staff.")
@SecurityRequirement(name = "bearer-jwt")
public class ChatRestController {

    private final ChatService chatService;
    private final CurrentUserService currentUserService;
    private final EmployeeSpecialtyRepository employeeSpecialtyRepository;

    record CreateThreadRequest(String category) {}

    @GetMapping("/threads")
    public List<ChatThreadDto> openThreads(@RequestParam(required = false) String category) {
        return chatService.openThreads(category);
    }

    @GetMapping("/threads/archived")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ChatThreadDto> archivedThreads(@RequestParam(required = false) String category) {
        return chatService.archivedThreads(category);
    }

    @PostMapping("/threads")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatThreadDto createThread(@RequestBody(required = false) CreateThreadRequest req) {
        String category = (req != null && req.category() != null) ? req.category() : "general";
        return chatService.createThread(category);
    }

    @GetMapping("/threads/me/open")
    public ChatThreadDto myOpenThread() {
        return chatService.getOrCreateOpenThreadForCustomer();
    }

    @GetMapping("/threads/{threadId}/messages")
    public List<ChatMessageDto> messages(@PathVariable Integer threadId) {
        return chatService.messages(threadId);
    }

    @PostMapping("/threads/{threadId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageDto post(@PathVariable Integer threadId,
                               @Valid @RequestBody PostChatMessageRequest req) {
        return chatService.postMessage(threadId, req);
    }

    @PostMapping("/threads/{threadId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ChatThreadDto assign(@PathVariable Integer threadId) {
        return chatService.assignEmployee(threadId);
    }

    @PostMapping("/threads/{threadId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Integer threadId) {
        chatService.markRead(threadId);
    }

    @PostMapping("/threads/{threadId}/close")
    public ChatThreadDto close(@PathVariable Integer threadId) {
        return chatService.closeThread(threadId);
    }

    @GetMapping("/staff/me/specialties")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<String> mySpecialties() {
        com.sait.peelin.model.User me = currentUserService.requireUser();
        return employeeSpecialtyRepository.findByUserId(me.getUserId()).stream()
                .map(com.sait.peelin.model.EmployeeSpecialty::getCategory)
                .toList();
    }
}
