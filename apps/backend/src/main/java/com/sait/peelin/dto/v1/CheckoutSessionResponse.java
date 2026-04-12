package com.sait.peelin.dto.v1;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutSessionResponse(
        UUID orderId,
        String orderNumber,
        String clientSecret,
        String paymentIntentId,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal deliveryFee,
        BigDecimal taxAmount,
        BigDecimal grandTotal
) {}
