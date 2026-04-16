package com.sait.peelin.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds HTTP Cache-Control headers to public read-only GET endpoints.
 * Any platform (web, Android, etc.) benefits from client-side caching
 * without code changes — standard HTTP semantics.
 */
@Configuration
public class HttpCacheConfig implements WebMvcConfigurer {

    private static final Map<String, CacheControl> CACHE_POLICIES = new LinkedHashMap<>();

    static {
        CACHE_POLICIES.put("/api/v1/bakeries",         CacheControl.noCache().cachePublic());
        CACHE_POLICIES.put("/api/v1/products",         CacheControl.noCache().cachePublic());
        CACHE_POLICIES.put("/api/v1/product-specials", CacheControl.noCache().cachePublic());
        CACHE_POLICIES.put("/api/v1/tags",             CacheControl.noCache().cachePublic());
        CACHE_POLICIES.put("/api/v1/reward-tiers",     CacheControl.noCache().cachePublic());
        CACHE_POLICIES.put("/api/v1/reviews/top",      CacheControl.noCache().cachePublic());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                     Object handler) {
                if (!"GET".equalsIgnoreCase(request.getMethod())) return true;

                String uri = request.getRequestURI();
                for (var entry : CACHE_POLICIES.entrySet()) {
                    if (uri.startsWith(entry.getKey())) {
                        response.setHeader("Cache-Control", entry.getValue().getHeaderValue());
                        break;
                    }
                }
                return true;
            }
        }).addPathPatterns(
                "/api/v1/bakeries/**",
                "/api/v1/products/**",
                "/api/v1/product-specials/**",
                "/api/v1/tags/**",
                "/api/v1/reward-tiers/**",
                "/api/v1/reviews/top"
        );
    }
}
