package com.sait.peelin.controller.ws;

import com.sait.peelin.dto.v1.TypingPayload;
import com.sait.peelin.service.StaffMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class StaffMessageWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final StaffMessageService staffMessageService;

    @MessageMapping("/messages/conversation/{convoId}/typing")
    public void typing(@DestinationVariable Integer convoId, TypingPayload payload) {
        messagingTemplate.convertAndSend(
                "/topic/messages/conversation/" + convoId + "/typing", payload);
    }

    @MessageMapping("/messages/conversation/{convoId}/read")
    public void markRead(@DestinationVariable Integer convoId) {
        staffMessageService.markRead(convoId);
    }
}
