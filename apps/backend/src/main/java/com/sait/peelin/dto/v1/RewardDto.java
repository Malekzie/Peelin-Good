package com.sait.peelin.dto.v1;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RewardDto(
        UUID id,
        UUID customerId,
        UUID orderId,
        int pointsEarned,
        OffsetDateTime transactionDate
) {}
