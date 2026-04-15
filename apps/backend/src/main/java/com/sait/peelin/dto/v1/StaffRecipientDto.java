package com.sait.peelin.dto.v1;

import java.util.UUID;

public record StaffRecipientDto(UUID userId, String username, String role) {}
