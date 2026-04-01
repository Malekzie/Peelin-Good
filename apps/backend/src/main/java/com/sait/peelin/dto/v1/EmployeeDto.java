package com.sait.peelin.dto.v1;

import java.util.UUID;

public record EmployeeDto(
        UUID id,
        UUID userId,
        Integer bakeryId,
        String firstName,
        String middleInitial,
        String lastName,
        String position,
        String phone,
        String workEmail,
        Integer addressId,
        AddressDto address,
        String profilePhotoPath,
        boolean photoApprovalPending
) {}
