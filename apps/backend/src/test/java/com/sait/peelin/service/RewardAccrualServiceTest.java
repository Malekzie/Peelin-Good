package com.sait.peelin.service;

import com.sait.peelin.model.Customer;
import com.sait.peelin.model.Order;
import com.sait.peelin.model.Reward;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.RewardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardAccrualServiceTest {

    @Mock private RewardRepository rewardRepository;
    @Mock private CustomerRepository customerRepository;

    @InjectMocks
    private RewardAccrualService rewardAccrualService;

    @Test
    void pointsEarnedForSubtotal_nineDollars_is9000() {
        assertEquals(9000, rewardAccrualService.pointsEarnedForSubtotal(new BigDecimal("9.00")));
    }

    @Test
    void pointsEarnedForSubtotal_roundsDownFractionalCents() {
        assertEquals(9990, rewardAccrualService.pointsEarnedForSubtotal(new BigDecimal("9.99")));
    }

    @Test
    void pointsEarnedForSubtotal_minimumOneWhenTiny() {
        assertEquals(1, rewardAccrualService.pointsEarnedForSubtotal(new BigDecimal("0.0001")));
    }

    @Test
    void grantEarnedPoints_savesRewardAndIncrementsBalance() {
        UUID orderId = UUID.randomUUID();
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setCustomerRewardBalance(100);

        Order order = new Order();
        order.setId(orderId);
        order.setCustomer(c);
        order.setOrderTotal(new BigDecimal("5.00"));

        when(rewardRepository.save(any(Reward.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        rewardAccrualService.grantEarnedPointsForPaidOrder(order);

        ArgumentCaptor<Reward> rewardCap = ArgumentCaptor.forClass(Reward.class);
        verify(rewardRepository).save(rewardCap.capture());
        assertEquals(5000, rewardCap.getValue().getRewardPointsEarned());

        ArgumentCaptor<Customer> custCap = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(custCap.capture());
        assertEquals(5100, custCap.getValue().getCustomerRewardBalance());
    }

    @Test
    void reverseEarnedPoints_deletesRewardsAndSubtractsBalanceFlooredAtZero() {
        UUID orderId = UUID.randomUUID();
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setCustomerRewardBalance(9000);

        Order order = new Order();
        order.setId(orderId);
        order.setCustomer(c);

        Reward r = new Reward();
        r.setRewardPointsEarned(9000);

        when(rewardRepository.findByOrder_Id(orderId)).thenReturn(List.of(r));

        rewardAccrualService.reverseEarnedPointsForOrder(order);

        verify(rewardRepository).deleteAll(List.of(r));
        ArgumentCaptor<Customer> custCap = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(custCap.capture());
        assertEquals(0, custCap.getValue().getCustomerRewardBalance());
    }

    @Test
    void reverseEarnedPoints_neverNegativeBalance() {
        Customer c = new Customer();
        c.setCustomerRewardBalance(100);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomer(c);

        Reward r = new Reward();
        r.setRewardPointsEarned(9000);

        when(rewardRepository.findByOrder_Id(order.getId())).thenReturn(List.of(r));

        rewardAccrualService.reverseEarnedPointsForOrder(order);

        ArgumentCaptor<Customer> custCap = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(custCap.capture());
        assertEquals(0, custCap.getValue().getCustomerRewardBalance());
    }
}
