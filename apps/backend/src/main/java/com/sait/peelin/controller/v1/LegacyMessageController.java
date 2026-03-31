package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.LegacyMessageDto;
import com.sait.peelin.dto.v1.SendLegacyMessageRequest;
import com.sait.peelin.service.LegacyMessageService;
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
@Tag(name = "Legacy messages")
public class LegacyMessageController {

    private final LegacyMessageService legacyMessageService;

    @GetMapping
    public List<LegacyMessageDto> mine() {
        return legacyMessageService.myMessages();
    }

    @GetMapping("/with/{userId}")
    public List<LegacyMessageDto> conversation(@PathVariable UUID userId) {
        return legacyMessageService.conversation(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LegacyMessageDto send(@Valid @RequestBody SendLegacyMessageRequest req) {
        return legacyMessageService.send(req);
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID id) {
        legacyMessageService.markRead(id);
    }
}
