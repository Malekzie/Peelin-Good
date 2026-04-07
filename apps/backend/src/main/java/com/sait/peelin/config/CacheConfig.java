package com.sait.peelin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        // Include type info so cached objects can be deserialized back to their correct DTO class
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(mapper)));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("tags",             defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("products",         defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("bakeries",         defaultConfig.entryTtl(Duration.ofMinutes(20)));
        // Renamed from product-specials: old Redis entries used a different JSON shape than current typing.
        cacheConfigurations.put("product-specials-v2", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("orders",           defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("rewards",          defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("analytics",        defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("dashboard",        defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("customers",        defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("employees",        defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("current-users",    defaultConfig.entryTtl(Duration.ofMinutes(1)));
        cacheConfigurations.put("reward-tiers",     defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("reviews",          defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                // Stale or cross-version Redis payloads may not deserialize; source load repopulates cache.
                if (log.isDebugEnabled()) {
                    log.debug("Cache read failed for '{}::{}', using source: {}", cache.getName(), key, e.getMessage());
                }
            }
        };
    }
}
