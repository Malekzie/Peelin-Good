package com.sait.peelin.controller.v1;

import com.sait.peelin.model.*;
import com.sait.peelin.repository.OrderRepository;
import com.sait.peelin.repository.PaymentRepository;
import com.sait.peelin.service.RewardAccrualService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RewardAccrualService rewardAccrualService;

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<String> handleWebhook(
            @RequestBody byte[] payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        final String payloadStr = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
        Event event;
        try {
            if (StringUtils.hasText(webhookSecret) && StringUtils.hasText(sigHeader)) {
                event = Webhook.constructEvent(payloadStr, sigHeader, webhookSecret);
            } else {
                log.warn("Stripe webhook signature not verified (STRIPE_WEBHOOK_SECRET not set)");
                event = Event.GSON.fromJson(payloadStr, Event.class);
            }
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Failed to parse Stripe webhook event", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Bad payload");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            String paymentIntentId = extractPaymentIntentId(event, payloadStr);
            if (paymentIntentId != null) {
                fulfillOrderByPaymentIntentId(paymentIntentId);
            } else {
                log.warn("payment_intent.succeeded event {}: could not read PaymentIntent id (deserializer + JSON fallback)",
                        event.getId());
            }
        }

        return ResponseEntity.ok("received");
    }

    /**
     * Stripe API versions newer than the bundled stripe-java model often leave
     * {@code Event.getDataObjectDeserializer().getObject()} empty; parse {@code data.object.id} from the raw payload.
     */
    private static String extractPaymentIntentId(Event event, String payloadStr) {
        Optional<StripeObject> objectOpt = event.getDataObjectDeserializer().getObject();
        if (objectOpt.isPresent() && objectOpt.get() instanceof PaymentIntent pi) {
            return pi.getId();
        }
        try {
            JsonObject root = JsonParser.parseString(payloadStr).getAsJsonObject();
            JsonObject data = root.getAsJsonObject("data");
            if (data == null) {
                return null;
            }
            JsonObject obj = data.getAsJsonObject("object");
            if (obj == null || !obj.has("object") || !obj.has("id")) {
                return null;
            }
            if (!"payment_intent".equals(obj.get("object").getAsString())) {
                return null;
            }
            return obj.get("id").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private void fulfillOrderByPaymentIntentId(String paymentIntentId) {
        Optional<Payment> paymentOpt = paymentRepository.findByStripeSessionId(paymentIntentId);
        if (paymentOpt.isEmpty()) {
            log.warn("No payment found for Stripe PaymentIntent {}", paymentIntentId);
            return;
        }

        Payment payment = paymentOpt.get();
        if (payment.getPaymentStatus() == PaymentStatus.completed) {
            log.info("Payment already fulfilled for PaymentIntent {}", paymentIntentId);
            return;
        }

        payment.setPaymentStatus(PaymentStatus.completed);
        payment.setPaymentPaidAt(OffsetDateTime.now());
        payment.setPaymentTransactionId(paymentIntentId);
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setOrderStatus(OrderStatus.paid);
        orderRepository.save(order);

        rewardAccrualService.grantEarnedPointsForPaidOrder(order);

        log.info("Order {} fulfilled via PaymentIntent {}", order.getId(), paymentIntentId);
    }
}
