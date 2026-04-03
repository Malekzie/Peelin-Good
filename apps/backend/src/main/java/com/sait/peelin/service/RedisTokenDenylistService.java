package com.sait.peelin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Profile("!dev")
@RequiredArgsConstructor
public class RedisTokenDenylistService implements TokenDenylistService {

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    private static final String PREFIX = "token-denylist:";

    @Override
    public void deny(String token) {
        Duration remaining = jwtService.getTimeUntilExpiry(token);
        if (!remaining.isZero() && !remaining.isNegative()) {
            redisTemplate.opsForValue().set(PREFIX + token, "1", remaining);
        }
    }

    @Override
    public boolean isDenied(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}
