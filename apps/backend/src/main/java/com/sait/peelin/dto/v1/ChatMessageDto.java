package com.sait.peelin.dto.v1;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatMessageDto(
        Integer id,
        Integer threadId,
        UUID senderUserId,
        String senderUsername,
        String senderRole,
        String text,
        OffsetDateTime sentAt,
        boolean read
) {}