package com.sait.peelin.service;

import com.sait.peelin.dto.v1.RewardTierDto;
import com.sait.peelin.dto.v1.RewardTierUpsertRequest;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.RewardTier;
import com.sait.peelin.repository.RewardTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RewardTierService {

    private final RewardTierRepository rewardTierRepository;

    public List<RewardTierDto> list() {
        return rewardTierRepository.findAll().stream().map(this::toDto).toList();
    }

    public RewardTierDto get(Integer id) {
        return toDto(rewardTierRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reward tier not found")));
    }

    @Transactional
    public RewardTierDto create(RewardTierUpsertRequest req) {
        RewardTier t = new RewardTier();
        apply(req, t);
        return toDto(rewardTierRepository.save(t));
    }

    @Transactional
    public RewardTierDto update(Integer id, RewardTierUpsertRequest req) {
        RewardTier t = rewardTierRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reward tier not found"));
        apply(req, t);
        return toDto(rewardTierRepository.save(t));
    }

    @Transactional
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
