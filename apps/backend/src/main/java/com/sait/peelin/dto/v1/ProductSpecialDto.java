package com.sait.peelin.dto.v1;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductSpecialDto(
        Integer productSpecialId,
        LocalDate featuredOn,
        BigDecimal discountPercent,
        Integer productId,
        String productName,
        String productDescription,
        BigDecimal productBasePrice,
        String productImageUrl
) {}
