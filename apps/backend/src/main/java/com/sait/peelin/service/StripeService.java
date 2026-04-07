package com.sait.peelin.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class StripeService {

    @Value("${stripe.secret-key:}")
    private String secretKey;

    /**
     * When false, checkout skips live Stripe calls so local environments without
     * {@code STRIPE_SECRET_KEY} can still place orders.
     */
    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    public PaymentIntent createPaymentIntent(UUID orderId, BigDecimal total) throws StripeException {
        Stripe.apiKey = secretKey;

        long amountCents = total
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency("cad")
                .putMetadata("orderId", orderId.toString())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        return PaymentIntent.create(params);
    }
}
