package com.sait.peelin.repository;

import com.sait.peelin.model.RewardTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RewardTierRepository extends JpaRepository<RewardTier, Integer> {
    Optional<RewardTier> findFirstByOrderByRewardTierMinPointsAsc();
}
