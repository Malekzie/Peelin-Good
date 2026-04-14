package com.sait.peelin.dto.v1;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StaffMessageDto(
        Integer id,
        Integer conversationId,
        UUID senderUserId,
        String text,
        OffsetDateTime sentAt,
        boolean read
) {}
