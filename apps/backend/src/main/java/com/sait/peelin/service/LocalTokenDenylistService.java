package com.sait.peelin.service;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile("dev")
public class LocalTokenDenylistService implements TokenDenylistService {

    @Override
    public void deny(String token) {
        // No-op (we don't persist anything locally)
    }

    @Override
    public boolean isDenied(String token) {
        return false; // Always allow for demo
    }
}