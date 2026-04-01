package com.sait.peelin.dto.v1;

import com.sait.peelin.model.UserRole;

import java.util.UUID;

public record UserSummaryDto(
        UUID id,
        String username,
        String email,
        UserRole role,
        boolean active
) {}
