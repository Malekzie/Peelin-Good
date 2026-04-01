package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeePatchRequest(
        String firstName,
        @Size(max = 1) @Pattern(regexp = "^$|^[A-Za-z]$", message = "Middle initial must be a single letter") String middleInitial,
        String lastName,
        String phone,
        String businessPhone,
        String workEmail,
        Integer addressId,
        AddressUpsertRequest address
) {}
