package com.sait.peelin.dto.v1;

import com.sait.peelin.model.ReviewStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReviewDto(
        UUID id,
        UUID customerId,
        Integer productId,
        UUID employeeId,
        short rating,
        String comment,
        ReviewStatus status,
        OffsetDateTime submittedAt,
        OffsetDateTime approvalDate
) {}
