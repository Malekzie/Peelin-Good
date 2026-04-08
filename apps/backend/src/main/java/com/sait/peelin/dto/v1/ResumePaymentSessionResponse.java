package com.sait.peelin.dto.v1;

import java.util.UUID;

/**
 * Result of resuming checkout for an order still in {@code pending_payment}.
 * When {@code orderPaid} is true, payment was already successful at Stripe and the order was just synced—no sheet.
 */
public record ResumePaymentSessionResponse(
        UUID orderId,
        String orderNumber,
        String clientSecret,
        String paymentIntentId,
        boolean orderPaid
) {}
