package com.sait.peelin.service;

import com.sait.peelin.dto.v1.RewardDto;
import com.sait.peelin.exception.ResourceNotFoundException;
import com.sait.peelin.model.Reward;
import com.sait.peelin.model.User;
import com.sait.peelin.model.UserRole;
import com.sait.peelin.repository.CustomerRepository;
import com.sait.peelin.repository.RewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RewardQueryService {

    private final RewardRepository rewardRepository;
    private final CustomerRepository customerRepository;
    private final CurrentUserService currentUserService;
    private final CustomerLookupCacheService customerLookupCacheService;

    @Transactional(readOnly = true)
    public List<RewardDto> listForCustomer(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found");
        }
        User u = currentUserService.requireUser();
        if (u.getUserRole() == UserRole.customer) {
            var self = customerLookupCacheService.findByUserId(u.getUserId());
            if (self == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            if (!self.getId().equals(customerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        return getCachedRewardsForCustomer(customerId);
    }

    @Cacheable(value = "rewards", key = "#customerId")
    public List<RewardDto> getCachedRewardsForCustomer(UUID customerId) {
        return rewardRepository.findByCustomer_IdOrderByRewardTransactionDateDesc(customerId).stream()
                .map(this::toDto)
                .toList();
    }
    public List<RewardDto> listAll() {
        return rewardRepository.findAll().stream().map(this::toDto).toList();
    }


    private RewardDto toDto(Reward r) {
        return new RewardDto(
                r.getId(),
                r.getCustomer().getId(),
                r.getOrder().getId(),
                r.getRewardPointsEarned(),
                r.getRewardTransactionDate()
        );
    }
}
