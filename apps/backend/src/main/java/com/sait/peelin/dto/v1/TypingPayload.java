package com.sait.peelin.dto.v1;

import java.util.UUID;

public record TypingPayload(UUID userId, boolean typing) {}
