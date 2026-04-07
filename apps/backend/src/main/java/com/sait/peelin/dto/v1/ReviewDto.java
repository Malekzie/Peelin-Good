package com.sait.peelin.dto.v1;

import com.sait.peelin.model.ReviewStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReviewDto(
        UUID id,
        UUID customerId,
        UUID orderId,
        Integer bakeryId,
        String bakeryName,
        Integer productId,
        UUID employeeId,
        short rating,
        String comment,
        ReviewStatus status,
        OffsetDateTime submittedAt,
        OffsetDateTime approvalDate,
        /** Public storefront label, e.g. "James R." (first name + last initial). */
        String reviewerDisplayName
) {}
