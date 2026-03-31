package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EmployeeUpsertRequest(
        @NotNull UUID userId,
        @NotNull Integer bakeryId,
        @NotNull Integer addressId,
        @NotBlank @Size(max = 50) String firstName,
        @Size(max = 2) String middleInitial,
        @NotBlank @Size(max = 50) String lastName,
        @NotBlank @Size(max = 40) String position,
        @NotBlank @Size(max = 20) String phone,
        @Size(max = 20) String businessPhone,
        @NotBlank @Size(max = 254) String workEmail
) {}
