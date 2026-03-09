package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reward")
public class Reward {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "reward_id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull
    @Column(name = "reward_points_earned", nullable = false)
    private Integer rewardPointsEarned;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "reward_transaction_date", nullable = false)
    private OffsetDateTime rewardTransactionDate;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Integer getRewardPointsEarned() {
        return rewardPointsEarned;
    }

    public void setRewardPointsEarned(Integer rewardPointsEarned) {
        this.rewardPointsEarned = rewardPointsEarned;
    }

    public OffsetDateTime getRewardTransactionDate() {
        return rewardTransactionDate;
    }

    public void setRewardTransactionDate(OffsetDateTime rewardTransactionDate) {
        this.rewardTransactionDate = rewardTransactionDate;
    }

}