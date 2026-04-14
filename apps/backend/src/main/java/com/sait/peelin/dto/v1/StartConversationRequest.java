package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartConversationRequest(@NotNull UUID recipientId) {}
