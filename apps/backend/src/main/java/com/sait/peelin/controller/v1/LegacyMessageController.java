package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.LegacyMessageDto;
import com.sait.peelin.dto.v1.SendLegacyMessageRequest;
import com.sait.peelin.service.LegacyMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Tag(name = "Legacy messages", description = "Simple direct messaging between users (pre-chat system). Kept for backward compatibility.")
@SecurityRequirement(name = "bearer-jwt")
public class LegacyMessageController {

    private final LegacyMessageService legacyMessageService;

    @Operation(summary = "List my messages", description = "Returns all messages sent to or from the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @GetMapping
    public List<LegacyMessageDto> mine() {
        return legacyMessageService.myMessages();
    }

    @Operation(summary = "Get conversation with user", description = "Returns the full message history between the authenticated user and another user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversation returned"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @GetMapping("/with/{userId}")
    public List<LegacyMessageDto> conversation(@PathVariable UUID userId) {
        return legacyMessageService.conversation(userId);
    }

    @Operation(summary = "Send message", description = "Send a direct message to another user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message sent"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LegacyMessageDto send(@Valid @RequestBody SendLegacyMessageRequest req) {
        return legacyMessageService.send(req);
    }

    @Operation(summary = "Mark message as read", description = "Mark a specific message as read by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Message marked as read"),
            @ApiResponse(responseCode = "404", description = "Message not found", content = @Content)
    })
    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID id) {
        legacyMessageService.markRead(id);
    }
}
