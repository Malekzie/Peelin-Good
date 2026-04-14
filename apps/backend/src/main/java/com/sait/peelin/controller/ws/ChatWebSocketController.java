package com.sait.peelin.controller.ws;

import com.sait.peelin.dto.v1.TypingPayload;
import com.sait.peelin.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat/thread/{threadId}/typing")
    public void typing(@DestinationVariable Integer threadId, TypingPayload payload) {
        messagingTemplate.convertAndSend(
                "/topic/chat/thread/" + threadId + "/typing", payload);
    }

    @MessageMapping("/chat/thread/{threadId}/read")
    public void markRead(@DestinationVariable Integer threadId) {
        chatService.markRead(threadId);
    }
}
