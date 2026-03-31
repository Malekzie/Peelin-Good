package com.sait.peelin.dto.v1;

import java.util.UUID;

public record CustomerDto(
        UUID id,
        UUID userId,
        Integer rewardTierId,
        String firstName,
        String middleInitial,
        String lastName,
        String phone,
        String email,
        int rewardBalance,
        Integer addressId,
        AddressDto address,
        String profilePhotoPath,
        boolean photoApprovalPending
) {}
