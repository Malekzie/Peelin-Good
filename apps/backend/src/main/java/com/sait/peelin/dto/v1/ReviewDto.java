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
        /** When the review became approved (staff action or auto-approved on submit). */
        OffsetDateTime approvalDate,
        /** Public storefront label, e.g. "James R." (first name + last initial). */
        String reviewerDisplayName,
        /**
         * When status is {@code rejected}, brief reason from AI moderation (or API failure message).
         * Rejected moderation outcomes are persisted for one-attempt review policy.
         */
        String moderationMessage
) {}
