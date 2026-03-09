package com.sait.peelin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "reward_tier")
public class RewardTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_tier_id", nullable = false)
    private Integer id;

    @Size(max = 30)
    @NotNull
    @Column(name = "reward_tier_name", nullable = false, length = 30)
    private String rewardTierName;

    @NotNull
    @Column(name = "reward_tier_min_points", nullable = false)
    private Integer rewardTierMinPoints;

    @Column(name = "reward_tier_max_points")
    private Integer rewardTierMaxPoints;

    @Column(name = "reward_tier_discount_rate", precision = 5, scale = 2)
    private BigDecimal rewardTierDiscountRate;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRewardTierName() {
        return rewardTierName;
    }

    public void setRewardTierName(String rewardTierName) {
        this.rewardTierName = rewardTierName;
    }

    public Integer getRewardTierMinPoints() {
        return rewardTierMinPoints;
    }

    public void setRewardTierMinPoints(Integer rewardTierMinPoints) {
        this.rewardTierMinPoints = rewardTierMinPoints;
    }

    public Integer getRewardTierMaxPoints() {
        return rewardTierMaxPoints;
    }

    public void setRewardTierMaxPoints(Integer rewardTierMaxPoints) {
        this.rewardTierMaxPoints = rewardTierMaxPoints;
    }

    public BigDecimal getRewardTierDiscountRate() {
        return rewardTierDiscountRate;
    }

    public void setRewardTierDiscountRate(BigDecimal rewardTierDiscountRate) {
        this.rewardTierDiscountRate = rewardTierDiscountRate;
    }

}