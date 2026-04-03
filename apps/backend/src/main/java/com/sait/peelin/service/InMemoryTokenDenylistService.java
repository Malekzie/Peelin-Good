package com.sait.peelin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class InMemoryTokenDenylistService implements TokenDenylistService {

    private final JwtService jwtService;

    private final ConcurrentHashMap<String, Instant> denied = new ConcurrentHashMap<>();

    @Override
    public void deny(String token) {
        Instant expiry = jwtService.extractExpiration(token);
        if (expiry != null && expiry.isAfter(Instant.now())) {
            denied.put(token, expiry);
        }
    }

    @Override
    public boolean isDenied(String token) {
        Instant expiry = denied.get(token);
        if (expiry == null) return false;
        if (Instant.now().isAfter(expiry)) {
            denied.remove(token);
            return false;
        }
        return true;
    }
}
