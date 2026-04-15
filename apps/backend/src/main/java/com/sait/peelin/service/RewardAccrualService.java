package com.sait.peelin.service;

import com.sait.peelin.model.Customer;
import com.sait.peelin.model.Order;
import com.sait.peelin.model.Reward;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.RewardRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Earned points = {@code floor(order_subtotal × {@link #POINTS_PER_DOLLAR})}, minimum 1.
 */
@Service
@RequiredArgsConstructor
public class RewardAccrualService {

    private static final Logger log = LoggerFactory.getLogger(RewardAccrualService.class);

    /** Points earned per $1.00 of order subtotal (after line-item / tier discount, before tax). */
    public static final int POINTS_PER_DOLLAR = 1000;

    private final RewardRepository rewardRepository;
    private final CustomerRepository customerRepository;
    private final CustomerLookupCacheService customerLookupCacheService;

    public int pointsEarnedForSubtotal(BigDecimal orderSubtotal) {
        if (orderSubtotal == null) {
            return 1;
        }
        BigDecimal multiplied = orderSubtotal.multiply(BigDecimal.valueOf(POINTS_PER_DOLLAR));
        int points = multiplied.setScale(0, RoundingMode.DOWN).intValue();
        return Math.max(points, 1);
    }

    @Transactional
    @CacheEvict(value = {"rewards", "customers"}, allEntries = true)
    public void grantEarnedPointsForPaidOrder(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }
        if (rewardRepository.existsByOrder_Id(order.getId())) {
            // Idempotent safety for webhook + confirm races.
            return;
        }
        Customer customer = order.getCustomer();
        if (customer == null) {
            log.warn("Order {} has no customer; skipping reward accrual", order.getId());
            return;
        }
        int points = pointsEarnedForSubtotal(order.getOrderTotal());

        Reward reward = new Reward();
        reward.setCustomer(customer);
        reward.setOrder(order);
        reward.setRewardPointsEarned(points);
        reward.setRewardTransactionDate(OffsetDateTime.now());
        rewardRepository.save(reward);

        int bal = customer.getCustomerRewardBalance() == null ? 0 : customer.getCustomerRewardBalance();
        customer.setCustomerRewardBalance(bal + points);
        customerRepository.save(customer);
        if (customer.getUser() != null && customer.getUser().getUserId() != null) {
            customerLookupCacheService.evictByUserId(customer.getUser().getUserId());
        }
    }

    /**
     * Removes reward rows for this order and subtracts their points from the customer balance (floored at zero).
     * Idempotent if there are no reward rows (e.g. order was never paid).
     */
    @Transactional
    @CacheEvict(value = {"rewards", "customers"}, allEntries = true)
    public void reverseEarnedPointsForOrder(Order order) {
        Customer customer = order.getCustomer();
        if (customer == null) {
            return;
        }
        List<Reward> rewards = rewardRepository.findByOrder_Id(order.getId());
        if (rewards.isEmpty()) {
            return;
        }
        int totalReversal = rewards.stream().mapToInt(Reward::getRewardPointsEarned).sum();
        rewardRepository.deleteAll(rewards);

        int bal = customer.getCustomerRewardBalance() == null ? 0 : customer.getCustomerRewardBalance();
        customer.setCustomerRewardBalance(Math.max(0, bal - totalReversal));
        customerRepository.save(customer);
        if (customer.getUser() != null && customer.getUser().getUserId() != null) {
            customerLookupCacheService.evictByUserId(customer.getUser().getUserId());
        }
    }
}
