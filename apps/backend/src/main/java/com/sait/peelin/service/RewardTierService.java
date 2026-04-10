package com.sait.peelin.service;

import com.sait.peelin.dto.v1.RewardTierDto;
import com.sait.peelin.dto.v1.RewardTierUpsertRequest;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.RewardTier;
import com.sait.peelin.repository.RewardTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RewardTierService {

    private final RewardTierRepository rewardTierRepository;

    @Cacheable("reward-tiers")
    public List<RewardTierDto> list() {
        return rewardTierRepository.findAll().stream().map(this::toDto).toList();
    }

    @Cacheable(value = "reward-tiers", key = "#id")
    public RewardTierDto get(Integer id) {
        return toDto(rewardTierRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reward tier not found")));
    }

    /**
     * Resolves the loyalty tier for a point balance from configured min/max ranges.
     * If multiple tiers match (bad data), the one with the highest {@code minPoints} wins.
     */
    @Transactional(readOnly = true)
    public Optional<RewardTier> tierForBalance(int balance) {
        return rewardTierRepository.findAll().stream()
                .filter(t -> {
                    int min = t.getRewardTierMinPoints() != null ? t.getRewardTierMinPoints() : 0;
                    Integer max = t.getRewardTierMaxPoints();
                    return balance >= min && (max == null || balance <= max);
                })
                .max(Comparator.comparing(t -> t.getRewardTierMinPoints() != null ? t.getRewardTierMinPoints() : 0));
    }

    @Transactional
    @CacheEvict(value = "reward-tiers", allEntries = true)
    public RewardTierDto create(RewardTierUpsertRequest req) {
        RewardTier t = new RewardTier();
        apply(req, t);
        return toDto(rewardTierRepository.save(t));
    }

    @Transactional
    @CacheEvict(value = "reward-tiers", allEntries = true)
    public RewardTierDto update(Integer id, RewardTierUpsertRequest req) {
        RewardTier t = rewardTierRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reward tier not found"));
        apply(req, t);
        return toDto(rewardTierRepository.save(t));
    }

    @Transactional
    @CacheEvict(value = "reward-tiers", allEntries = true)
    public void delete(Integer id) {
        if (!rewardTierRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reward tier not found");
        }
        rewardTierRepository.deleteById(id);
    }

    private void apply(RewardTierUpsertRequest req, RewardTier t) {
        t.setRewardTierName(req.getName().trim());
        t.setRewardTierMinPoints(req.getMinPoints());
        t.setRewardTierMaxPoints(req.getMaxPoints());
        t.setRewardTierDiscountRate(req.getDiscountRatePercent());
    }

    private RewardTierDto toDto(RewardTier t) {
        return new RewardTierDto(
                t.getId(),
                t.getRewardTierName(),
                t.getRewardTierMinPoints(),
                t.getRewardTierMaxPoints(),
                t.getRewardTierDiscountRate()
        );
    }
}
