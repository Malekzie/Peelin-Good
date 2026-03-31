package com.sait.peelin.dto.v1;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LegacyMessageDto(
        UUID id,
        UUID senderId,
        UUID receiverId,
        String subject,
        String content,
        OffsetDateTime sentAt,
        boolean read
) {}
