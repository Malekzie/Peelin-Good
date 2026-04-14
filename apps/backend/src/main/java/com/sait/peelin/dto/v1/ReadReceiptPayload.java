package com.sait.peelin.dto.v1;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReadReceiptPayload(UUID userId, OffsetDateTime readAt) {}
