package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.ChatMessageDto;
import com.sait.peelin.dto.v1.ChatThreadDto;
import com.sait.peelin.dto.v1.PostChatMessageRequest;
import com.sait.peelin.service.ChatService;
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
@Tag(name = "Chat")
public class ChatRestController {

    private final ChatService chatService;

    @GetMapping("/threads")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<ChatThreadDto> openThreads() {
        return chatService.openThreads();
    }

    @PostMapping("/threads")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatThreadDto createThread() {
        return chatService.createThread();
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
    public ChatMessageDto post(@PathVariable Integer threadId, @Valid @RequestBody PostChatMessageRequest req) {
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
}
