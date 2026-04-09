package com.sait.peelin.service;

import com.sait.peelin.model.User;
import com.sait.peelin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserLookupCacheService {

    private final UserRepository userRepository;

    public User findActiveByLoginIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }

        return userRepository.findByUsernameIgnoreCaseOrUserEmailIgnoreCase(identifier.trim(), identifier.trim())
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .orElse(null);
    }
}