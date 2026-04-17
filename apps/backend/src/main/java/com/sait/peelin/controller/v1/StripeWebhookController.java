package com.sait.peelin.controller.v1;

import com.sait.peelin.model.StripeProcessedEvent;
import com.sait.peelin.repository.StripeProcessedEventRepository;
import com.sait.peelin.service.StripePaymentFulfillmentService;
import com.sait.peelin.service.StripePaymentFulfillmentService.PaymentIntentSnapshot;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${stripe.publishable-key:}")
    private String publishableKey;

    private final StripePaymentFulfillmentService stripePaymentFulfillmentService;
    private final StripeProcessedEventRepository stripeProcessedEventRepository;

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> config() {
        return ResponseEntity.ok(Map.of("publishableKey", publishableKey != null ? publishableKey : ""));
    }

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<String> handleWebhook(
            @RequestBody byte[] payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        final String payloadStr = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
        Event event;
        try {
            // If the secret is configured we MUST verify — silently accepting unsigned events would
            // let any anonymous caller forge a payment_intent.succeeded for any order.
            if (StringUtils.hasText(webhookSecret)) {
                if (!StringUtils.hasText(sigHeader)) {
                    log.warn("Stripe webhook rejected: signature header missing");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing signature");
                }
                event = Webhook.constructEvent(payloadStr, sigHeader, webhookSecret);
            } else {
                // Dev-only path: STRIPE_WEBHOOK_SECRET is required in prod by EnvValidator,
                // so reaching this branch means we're on a local profile without `stripe listen`.
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

        // Stripe retries on any non-2xx and may also re-deliver successfully-acked events.
        // Persisting the event id under a PK uniqueness constraint guarantees we only fulfill once.
        if (!claimEvent(event.getId())) {
            log.info("Stripe webhook event {} already processed; returning 200", event.getId());
            return ResponseEntity.ok("duplicate");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntentSnapshot snapshot = extractPaymentIntent(payloadStr);
            if (snapshot == null) {
                log.warn("payment_intent.succeeded event {}: could not read PaymentIntent payload", event.getId());
            } else {
                stripePaymentFulfillmentService.fulfillOrderByPaymentIntent(snapshot);
            }
        }

        return ResponseEntity.ok("received");
    }

    /**
     * Insert event id; return false if it was already there. Caller must be inside a @Transactional.
     */
    private boolean claimEvent(String eventId) {
        if (stripeProcessedEventRepository.existsById(eventId)) {
            return false;
        }
        try {
            StripeProcessedEvent row = new StripeProcessedEvent();
            row.setEventId(eventId);
            row.setProcessedAt(OffsetDateTime.now());
            stripeProcessedEventRepository.save(row);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Lost a race with a concurrent delivery — treat as already processed.
            return false;
        }
    }

    /**
     * Stripe API versions newer than the bundled stripe-java model often leave
     * {@code Event.getDataObjectDeserializer().getObject()} empty; parse {@code data.object} fields
     * directly from the raw payload so we get id + amount + currency in one pass.
     */
    private static PaymentIntentSnapshot extractPaymentIntent(String payloadStr) {
        try {
            JsonObject root = JsonParser.parseString(payloadStr).getAsJsonObject();
            JsonObject data = root.getAsJsonObject("data");
            if (data == null) return null;
            JsonObject obj = data.getAsJsonObject("object");
            if (obj == null || !obj.has("object") || !obj.has("id") || !obj.has("amount")) {
                return null;
            }
            if (!"payment_intent".equals(obj.get("object").getAsString())) {
                return null;
            }
            String currency = obj.has("currency") ? obj.get("currency").getAsString() : null;
            return new PaymentIntentSnapshot(
                    obj.get("id").getAsString(),
                    obj.get("amount").getAsLong(),
                    currency);
        } catch (Exception e) {
            return null;
        }
    }
}
