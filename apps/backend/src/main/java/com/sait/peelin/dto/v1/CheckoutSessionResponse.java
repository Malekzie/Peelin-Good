package com.sait.peelin.dto.v1;

import java.util.UUID;

public record CheckoutSessionResponse(
        UUID orderId,
        String orderNumber,
        String clientSecret,
        String paymentIntentId
) {}
