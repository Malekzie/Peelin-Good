package com.sait.peelin.service;

import com.sait.peelin.model.User;
import com.sait.peelin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserLookupCacheService {

    private final UserRepository userRepository;

    @Cacheable(
            value = "current-users",
            key = "#identifier.toLowerCase()",
            condition = "#identifier != null && !#identifier.isBlank()",
            unless = "#result == null"
    )
    public User findActiveByLoginIdentifier(String identifier) {
        return userRepository.findByUsernameIgnoreCaseOrUserEmailIgnoreCase(identifier, identifier)
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .orElse(null);
    }

    @CacheEvict(
            value = "current-users",
            key = "#identifier.toLowerCase()",
            condition = "#identifier != null && !#identifier.isBlank()"
    )
    public void evictByIdentifier(String identifier) {
        // Annotation-driven eviction.
    }
}

