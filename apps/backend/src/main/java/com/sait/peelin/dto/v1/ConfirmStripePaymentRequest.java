package com.sait.peelin.dto.v1;

import jakarta.validation.constraints.NotBlank;

public record ConfirmStripePaymentRequest(
        @NotBlank String paymentIntentId
) {}
