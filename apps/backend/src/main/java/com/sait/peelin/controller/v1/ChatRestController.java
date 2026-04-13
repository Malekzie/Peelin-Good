package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.ChatMessageDto;
import com.sait.peelin.dto.v1.ChatThreadDto;
import com.sait.peelin.dto.v1.PostChatMessageRequest;
import com.sait.peelin.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Chat", description = "Real-time support chat between customers and staff. Staff endpoints require ADMIN or EMPLOYEE role.")
@SecurityRequirement(name = "bearer-jwt")
public class ChatRestController {

    private final ChatService chatService;

    @Operation(summary = "List open threads", description = "Returns all open chat threads awaiting or in active staff support. Requires ADMIN or EMPLOYEE role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Open threads returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping("/threads")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<ChatThreadDto> openThreads() {
        return chatService.openThreads();
    }

    @Operation(summary = "Create thread", description = "Open a new support chat thread for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Thread created"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @PostMapping("/threads")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatThreadDto createThread() {
        return chatService.createThread();
    }

    @Operation(summary = "Get my open thread", description = "Returns the authenticated customer's current open support thread, creating one if none exists.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Open thread returned or created"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @GetMapping("/threads/me/open")
    public ChatThreadDto myOpenThread() {
        return chatService.getOrCreateOpenThreadForCustomer();
    }

    @Operation(summary = "Get messages in thread", description = "Returns all messages in a chat thread, ordered chronologically.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages returned"),
            @ApiResponse(responseCode = "403", description = "Thread belongs to another user", content = @Content),
            @ApiResponse(responseCode = "404", description = "Thread not found", content = @Content)
    })
    @GetMapping("/threads/{threadId}/messages")
    public List<ChatMessageDto> messages(@PathVariable Integer threadId) {
        return chatService.messages(new ChatService.ThreadRef(threadId));
    }

    @Operation(summary = "Post message", description = "Send a message to a chat thread.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message sent"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Thread not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Thread is not open", content = @Content)
    })
    @PostMapping("/threads/{threadId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageDto post(@PathVariable Integer threadId, @Valid @RequestBody PostChatMessageRequest req) {
        return chatService.postMessage(new ChatService.ThreadRef(threadId), req);
    }

    @Operation(summary = "Assign thread to employee", description = "Assign the authenticated employee to a support thread. Requires ADMIN or EMPLOYEE role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thread assigned, updated thread returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Thread not found", content = @Content)
    })
    @PostMapping("/threads/{threadId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ChatThreadDto assign(@PathVariable Integer threadId) {
        return chatService.assignEmployee(new ChatService.ThreadRef(threadId));
    }

    @Operation(summary = "Mark thread as read", description = "Mark all messages in a thread as read by the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Thread marked as read"),
            @ApiResponse(responseCode = "404", description = "Thread not found", content = @Content)
    })
    @PostMapping("/threads/{threadId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Integer threadId) {
        chatService.markRead(new ChatService.ThreadRef(threadId));
    }
}