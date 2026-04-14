package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.SendStaffMessageRequest;
import com.sait.peelin.dto.v1.StaffConversationDto;
import com.sait.peelin.dto.v1.StaffMessageDto;
import com.sait.peelin.dto.v1.StartConversationRequest;
import com.sait.peelin.service.StaffMessageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
@Tag(name = "Staff Messages", description = "Staff-to-staff direct messaging.")
@SecurityRequirement(name = "bearer-jwt")
public class StaffMessageController {

    private final StaffMessageService staffMessageService;

    @GetMapping("/conversations")
    public List<StaffConversationDto> conversations() {
        return staffMessageService.conversations();
    }

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffConversationDto startConversation(@Valid @RequestBody StartConversationRequest req) {
        return staffMessageService.getOrCreateConversation(req.recipientId());
    }

    @GetMapping("/conversations/{convoId}/messages")
    public List<StaffMessageDto> messages(@PathVariable Integer convoId) {
        return staffMessageService.messages(convoId);
    }

    @PostMapping("/conversations/{convoId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffMessageDto send(@PathVariable Integer convoId,
                                @Valid @RequestBody SendStaffMessageRequest req) {
        return staffMessageService.sendMessage(convoId, req.text());
    }

    @PostMapping("/conversations/{convoId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Integer convoId) {
        staffMessageService.markRead(convoId);
    }
}
