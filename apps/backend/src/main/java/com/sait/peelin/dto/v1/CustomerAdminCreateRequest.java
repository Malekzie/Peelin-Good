package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Admin creates a customer profile, optionally linking a {@code customer}-role login that has no profile yet.
 */
public record CustomerAdminCreateRequest(
        /** When null, a guest customer (no login) is created. */
        UUID userId,
        @NotNull Integer addressId,
        @NotBlank @Size(max = 50) String firstName,
        @Size(max = 1)
        @Pattern(regexp = "^$|^[A-Za-z]$", message = "Middle initial must be a single letter")
        String middleInitial,
        @NotBlank @Size(max = 50) String lastName,
        @NotBlank @Size(max = 20) String phone,
        @Size(max = 20) String businessPhone,
        @NotBlank @Size(max = 254) String email,
        Integer rewardBalance
) {}
