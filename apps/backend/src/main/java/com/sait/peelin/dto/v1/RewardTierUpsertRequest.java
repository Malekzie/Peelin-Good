package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RewardTierUpsertRequest {
    @NotBlank
    private String name;
    @NotNull
    private Integer minPoints;
    private Integer maxPoints;
    private BigDecimal discountRatePercent;
}
