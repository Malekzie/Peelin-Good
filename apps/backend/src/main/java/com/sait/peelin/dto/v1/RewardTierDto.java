package com.sait.peelin.dto.v1;

import java.math.BigDecimal;

public record RewardTierDto(
        Integer id,
        String name,
        int minPoints,
        Integer maxPoints,
        BigDecimal discountRatePercent
) {}
