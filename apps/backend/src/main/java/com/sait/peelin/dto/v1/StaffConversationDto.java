package com.sait.peelin.dto.v1;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StaffConversationDto(
        Integer id,
        UUID otherUserId,
        String otherUsername,
        OffsetDateTime updatedAt,
        int unreadCount
) {}
