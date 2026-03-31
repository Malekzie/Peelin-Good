package com.sait.peelin.repository;

import com.sait.peelin.model.Reward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RewardRepository extends JpaRepository<Reward, UUID> {

    List<Reward> findByCustomer_IdOrderByRewardTransactionDateDesc(UUID customerId);
}
