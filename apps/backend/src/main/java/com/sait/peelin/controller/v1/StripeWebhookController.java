package com.sait.peelin.controller.v1;

import com.sait.peelin.model.*;
import com.sait.peelin.repository.*;
import com.sait.peelin.service.EmailService;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final OrderItemRepository orderItemRepository;
    private final RewardRepository rewardRepository;
    private final CustomerRepository customerRepository;
    private final EmailService emailService;

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<String> handleWebhook(
            @RequestBody byte[] payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        Event event;
        try {
            String payloadStr = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
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
            Optional<StripeObject> objectOpt = event.getDataObjectDeserializer().getObject();
            if (objectOpt.isPresent() && objectOpt.get() instanceof PaymentIntent paymentIntent) {
                fulfillOrder(paymentIntent);
            }
        }

        return ResponseEntity.ok("received");
    }

    private void fulfillOrder(PaymentIntent paymentIntent) {
        String paymentIntentId = paymentIntent.getId();
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

        Customer customer = order.getCustomer();
        BigDecimal total = order.getOrderTotal();
        int points = total.setScale(0, RoundingMode.DOWN).intValue();

        Reward reward = new Reward();
        reward.setCustomer(customer);
        reward.setOrder(order);
        reward.setRewardPointsEarned(Math.max(points, 1));
        reward.setRewardTransactionDate(OffsetDateTime.now());
        rewardRepository.save(reward);

        customer.setCustomerRewardBalance(customer.getCustomerRewardBalance() + reward.getRewardPointsEarned());
        customerRepository.save(customer);

        log.info("Order {} fulfilled via PaymentIntent {}", order.getId(), paymentIntentId);

        emailService.sendOrderConfirmation(order, orderItemRepository.findByOrder_Id(order.getId()));
    }
}
