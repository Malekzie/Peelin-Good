package com.sait.peelin.service;

import com.sait.peelin.repository.ChatThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatLookupCacheService {

    private final ChatThreadRepository chatThreadRepository;

    @Cacheable(
            value = "chat-open-thread-by-customer",
            key = "#customerUserId",
            condition = "#customerUserId != null",
            unless = "#result == null"
    )
    public Integer findOpenThreadIdForCustomer(UUID customerUserId) {
        return chatThreadRepository.findLatestOpenThreadIdByCustomerUserId(customerUserId);
    }

    @CacheEvict(value = "chat-open-thread-by-customer", key = "#customerUserId", condition = "#customerUserId != null")
    public void evictOpenThreadForCustomer(UUID customerUserId) {
        // Annotation-driven eviction.
    }
}

