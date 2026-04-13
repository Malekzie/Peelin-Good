package com.sait.peelin.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        // PROPERTY (@class inside JSON objects) matches legacy Redis entries; WRAPPER_ARRAY breaks old keys.
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.WRAPPER_ARRAY
        );

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(mapper)));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("tags",             defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("products",         defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put("bakeries",         defaultConfig.entryTtl(Duration.ofHours(3)));
        cacheConfigurations.put("product-specials", defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("orders",           defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("rewards",          defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("analytics",        defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put("dashboard",        defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("customers",        defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("customer-by-user-id", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("employees",        defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("current-users",    defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("chat-open-threads", defaultConfig.entryTtl(Duration.ofSeconds(15)));
        cacheConfigurations.put("chat-open-thread-by-customer", defaultConfig.entryTtl(Duration.ofSeconds(15)));
        cacheConfigurations.put("chat-messages",    defaultConfig.entryTtl(Duration.ofSeconds(15)));
        cacheConfigurations.put("reward-tiers",     defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("reviews",          defaultConfig.entryTtl(Duration.ofMinutes(30)));
        // recommendations: List<String> from RecommendationAiCacheService (legacy-compatible shape).
        cacheConfigurations.put("recommendations",  defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Bean
    public ApplicationRunner cacheFlushOnStartup(RedisCacheManager cacheManager) {
        return args -> {
            cacheManager.getCacheNames().forEach(name -> {
                Cache cache = cacheManager.getCache(name);
                if (cache != null) cache.clear();
            });
            log.info("Redis caches flushed on startup");
        };
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                // Stale or cross-version Redis payloads may not deserialize; treat as miss (do not rethrow).
                log.warn("Cache read failed for '{}::{}', loading from source: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                // Still return the computed value to the client (e.g. Redis briefly unavailable).
                log.warn("Cache put failed for '{}::{}': {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache evict failed for '{}::{}': {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Cache clear failed for '{}': {}", cache.getName(), e.getMessage());
            }
        };
    }
}
