package com.sait.peelin.dto.v1;

import java.math.BigDecimal;

public record OrderItemDto(
        Integer id,
        Integer productId,
        String productName,
        Integer batchId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
