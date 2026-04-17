package com.sait.peelin.service;

import com.sait.peelin.model.Order;
import com.sait.peelin.model.OrderItem;
import com.sait.peelin.model.OrderStatus;
import com.sait.peelin.model.Payment;
import com.sait.peelin.model.PaymentStatus;
import com.sait.peelin.repository.OrderItemRepository;
import com.sait.peelin.repository.OrderRepository;
import com.sait.peelin.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StripePaymentFulfillmentService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentFulfillmentService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RewardAccrualService rewardAccrualService;
    private final EmailService emailService;

    private static final String EXPECTED_CURRENCY = "cad";

    /**
     * Snapshot of the {@code data.object} payload for {@code payment_intent.succeeded}, carrying the
     * fields we need to defend against tampered or mismatched webhook deliveries.
     */
    public record PaymentIntentSnapshot(String id, long amountCents, String currency) {}

    /**
     * Webhook entrypoint: re-validates the PaymentIntent's amount + currency against the persisted
     * Payment row before delegating to the by-id fulfillment path. A mismatch leaves the order in
     * {@code pending_payment} so a human can investigate (could indicate a forged event or a
     * configuration drift between the dashboard's price and our DB).
     */
    @Transactional
    @CacheEvict(value = {"orders", "analytics", "dashboard"}, allEntries = true)
    public void fulfillOrderByPaymentIntent(PaymentIntentSnapshot snapshot) {
        if (snapshot == null || snapshot.id() == null) {
            log.warn("fulfillOrderByPaymentIntent called with null snapshot/id; ignoring");
            return;
        }
        Optional<Payment> paymentOpt = paymentRepository.findByStripeSessionId(snapshot.id());
        if (paymentOpt.isEmpty()) {
            log.warn("No payment found for Stripe PaymentIntent {}", snapshot.id());
            return;
        }
        Payment payment = paymentOpt.get();
        if (!matchesExpectedCurrency(snapshot)) {
            log.error("Currency mismatch for PaymentIntent {}: expected {} but Stripe reported {}; leaving order pending_payment",
                    snapshot.id(), EXPECTED_CURRENCY, snapshot.currency());
            return;
        }
        if (!matchesExpectedAmount(payment, snapshot)) {
            long expectedCents = payment.getPaymentAmount() != null
                    ? payment.getPaymentAmount().multiply(BigDecimal.valueOf(100)).longValueExact()
                    : -1L;
            log.error("Amount mismatch for PaymentIntent {}: expected {} cents but Stripe reported {} cents; leaving order pending_payment",
                    snapshot.id(), expectedCents, snapshot.amountCents());
            return;
        }
        fulfillOrderByPaymentIntentId(snapshot.id());
    }

    private static boolean matchesExpectedCurrency(PaymentIntentSnapshot snapshot) {
        return snapshot.currency() != null
                && EXPECTED_CURRENCY.equalsIgnoreCase(snapshot.currency());
    }

    private static boolean matchesExpectedAmount(Payment payment, PaymentIntentSnapshot snapshot) {
        if (payment.getPaymentAmount() == null) return false;
        long expectedCents = payment.getPaymentAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact();
        return expectedCents == snapshot.amountCents();
    }

    /**
     * Marks payment completed and order paid when Stripe reports success (webhook or client-driven confirm).
     * Idempotent if already completed.
     */
    @Transactional
    @CacheEvict(value = {"orders", "analytics", "dashboard"}, allEntries = true)
    public void fulfillOrderByPaymentIntentId(String paymentIntentId) {
        Optional<Payment> paymentOpt = paymentRepository.findByStripeSessionId(paymentIntentId);
        if (paymentOpt.isEmpty()) {
            log.warn("No payment found for Stripe PaymentIntent {}", paymentIntentId);
            return;
        }

        Payment payment = paymentOpt.get();
        Order order = payment.getOrder();
        if (payment.getPaymentStatus() == PaymentStatus.completed) {
            // Self-heal edge cases where payment was marked complete but reward/status was missed.
            if (order.getOrderStatus() != OrderStatus.paid) {
                order.setOrderStatus(OrderStatus.paid);
                orderRepository.save(order);
            }
            rewardAccrualService.grantEarnedPointsForPaidOrder(order);
            log.info("Payment already fulfilled for PaymentIntent {}", paymentIntentId);
            return;
        }

        payment.setPaymentStatus(PaymentStatus.completed);
        payment.setPaymentPaidAt(OffsetDateTime.now());
        payment.setPaymentTransactionId(paymentIntentId);
        paymentRepository.save(payment);

        order.setOrderStatus(OrderStatus.paid);
        orderRepository.save(order);

        rewardAccrualService.grantEarnedPointsForPaidOrder(order);

        List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());
        emailService.sendOrderConfirmation(order, items);

        log.info("Order {} fulfilled via PaymentIntent {}", order.getId(), paymentIntentId);
    }
}
