package com.sait.peelin.service;

import com.sait.peelin.model.Customer;
import com.sait.peelin.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerLookupCacheService {

    private final CustomerRepository customerRepository;

    @Cacheable(
            value = "customer-by-user-id",
            key = "#userId",
            condition = "#userId != null",
            unless = "#result == null"
    )
    public Customer findByUserId(UUID userId) {
        return customerRepository.findByUser_UserId(userId).orElse(null);
    }

    @CacheEvict(value = "customer-by-user-id", key = "#userId", condition = "#userId != null")
    public void evictByUserId(UUID userId) {
        // Annotation-driven eviction.
    }
}

