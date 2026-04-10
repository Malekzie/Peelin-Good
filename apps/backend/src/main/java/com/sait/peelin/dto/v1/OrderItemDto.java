package com.sait.peelin.dto.v1;

import java.math.BigDecimal;

public record OrderItemDto(
        Integer id,
        Integer productId,
        String productName,
        String productImageUrl,
        Integer batchId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
