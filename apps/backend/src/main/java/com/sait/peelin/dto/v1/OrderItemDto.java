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
        BigDecimal lineTotal,
        /** True if this customer already has a product-detail review attempt for this product (any status). */
        boolean productReviewSubmitted
) {}
