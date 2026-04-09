package com.sait.peelin.dto.v1;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerDto(
        UUID id,
        UUID userId,
        String username,
        Integer rewardTierId,
        String rewardTierName,
        /** Percent off eligible orders for this tier (e.g. 5 = 5%); mirrors reward_tier.discount. */
        BigDecimal rewardTierDiscountPercent,
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
