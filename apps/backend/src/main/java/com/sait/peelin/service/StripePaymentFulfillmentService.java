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
