package com.sait.peelin.config;

import org.springframework.cache.annotation.EnableCaching;
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
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("tags",             defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("products",         defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("bakeries",         defaultConfig.entryTtl(Duration.ofMinutes(20)));
        cacheConfigurations.put("product-specials", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("orders",           defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("rewards",          defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("analytics",        defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("dashboard",        defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("customers",        defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("employees",        defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("reward-tiers",     defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("reviews",          defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
