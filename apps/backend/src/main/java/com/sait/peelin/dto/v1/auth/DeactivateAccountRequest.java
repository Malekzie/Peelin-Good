package com.sait.peelin.dto.v1.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeactivateAccountRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;
}
