package com.sait.peelin.dto.v1;

import java.util.UUID;

public record CustomerDto(
        UUID id,
        UUID userId,
        String username,
        Integer rewardTierId,
        String firstName,
        String middleInitial,
        String lastName,
        String phone,
        String businessPhone,
        String email,
        int rewardBalance,
        Integer addressId,
        AddressDto address,
        String profilePhotoPath,
        boolean photoApprovalPending
) {}
