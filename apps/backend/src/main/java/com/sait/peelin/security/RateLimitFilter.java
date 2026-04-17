package com.sait.peelin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Per-IP rate limiter for unauthenticated auth + password-reset endpoints. Backed by the existing
 * Redis/Valkey instance using an atomic INCR + EXPIRE-on-first-hit Lua script. Returns 429 with a
 * Retry-After header when the bucket is exhausted.
 *
 * Single-attacker brute-force only — credential-stuffing across botnets needs a WAF/Cloudflare layer.
 */
@Component
@Profile("!test & !local-no-redis")
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(HttpMethod method, String path, int max, int windowSeconds) {}

    private static final List<Rule> RULES = List.of(
            new Rule(HttpMethod.POST, "/api/v1/auth/login", 10, 60),
            new Rule(HttpMethod.POST, "/api/v1/auth/register", 5, 60),
            new Rule(HttpMethod.POST, "/api/v1/auth/forgot-password", 3, 3600),
            new Rule(HttpMethod.POST, "/api/v1/auth/reset-password", 5, 900),
            new Rule(HttpMethod.GET, "/api/v1/auth/reset-password/validate", 20, 3600));

    // KEYS[1] = bucket key, ARGV[1] = window seconds. Returns the new count.
    private static final RedisScript<Long> INCR_AND_EXPIRE = new DefaultRedisScript<>(
            "local n = redis.call('INCR', KEYS[1]) "
                    + "if n == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "return n",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Rule rule = matchRule(request);
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);
        String key = "rl:" + rule.path() + ":" + ip;
        long count = incrAndCount(key, rule.windowSeconds());

        if (count > rule.max()) {
            writeTooManyRequests(response, rule.windowSeconds());
            return;
        }

        chain.doFilter(request, response);
    }

    private static Rule matchRule(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        for (Rule r : RULES) {
            if (r.method().name().equals(method) && r.path().equals(path)) {
                return r;
            }
        }
        return null;
    }

    private long incrAndCount(String key, int windowSeconds) {
        try {
            Long n = redisTemplate.execute(
                    INCR_AND_EXPIRE, List.of(key), String.valueOf(windowSeconds));
            return n == null ? 0L : n;
        } catch (Exception e) {
            // Fail-open: don't lock everyone out if Redis is briefly unavailable. Log so ops sees it.
            log.warn("Rate-limit backend unavailable, allowing request: {}", e.getMessage());
            return 0L;
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, int retryAfterSeconds)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        objectMapper.writeValue(response.getWriter(),
                Map.of("error", "Too many requests", "retryAfterSeconds", retryAfterSeconds));
    }
}
