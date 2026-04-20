package com.sait.peelin.dto.v1;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatThreadDto(
        Integer id,
        UUID customerUserId,
        String customerDisplayName,
        String customerUsername,
        String customerEmail,
        String customerProfilePhotoPath,
        Boolean customerPhotoApprovalPending,
        UUID employeeUserId,
        String employeeDisplayName,
        String employeeUsername,
        String employeeProfilePhotoPath,
        String status,
        String category,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime closedAt
) {}
