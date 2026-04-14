package com.sait.peelin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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

    /**
     * 🔥 REDIS CACHE (ONLY when profile = "redis")
     */
    @Bean
    @Profile("redis")
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(mapper)));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("tags", defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("products", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put("bakeries", defaultConfig.entryTtl(Duration.ofHours(3)));
        cacheConfigurations.put("product-specials-v2", defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("orders", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("rewards", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("analytics", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put("dashboard", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("customers", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("employees", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("current-users", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("reward-tiers", defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("reviews", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * 🔥 LOCAL CACHE (used in dev instead of Redis)
     */
    @Bean
    @Profile("!redis")
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "tags",
                "products",
                "bakeries",
                "product-specials-v2",
                "orders",
                "rewards",
                "analytics",
                "dashboard",
                "customers",
                "employees",
                "current-users",
                "reward-tiers",
                "reviews"
        );
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache GET failed for '{}::{}' - using source. Cause: {}",
                        cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Cache PUT failed for '{}::{}' - continuing without cache. Cause: {}",
                        cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache EVICT failed for '{}::{}' - continuing. Cause: {}",
                        cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Cache CLEAR failed for '{}' - continuing. Cause: {}",
                        cache.getName(), e.getMessage());
            }
        };
    }
}