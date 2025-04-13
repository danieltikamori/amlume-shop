/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config;

import me.amlu.shop.amlume_shop.commons.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static me.amlu.shop.amlume_shop.commons.Constants.PRODUCTS_CACHE_TTL;
import static me.amlu.shop.amlume_shop.commons.Constants.CATEGORIES_CACHE_TTL;
import static me.amlu.shop.amlume_shop.commons.Constants.USERS_CACHE_TTL;
import static me.amlu.shop.amlume_shop.commons.Constants.ROLES_CACHE_TTL;
import static me.amlu.shop.amlume_shop.commons.Constants.ASN_CACHE_TTL;
import static me.amlu.shop.amlume_shop.commons.Constants.TOKENS_CACHE_TTL;
import static me.amlu.shop.amlume_shop.commons.Constants.TEMPORARY_CACHE_TTL;


/**
 * Configuration class for setting up caching and Redis/Valkey connection
 * using Spring Data Redis with Lettuce connector.
 * Configures RedisTemplate, StringRedisTemplate, and the primary CacheManager beans.
 */
@Configuration
@EnableCaching // Enables Spring's caching annotations like @Cacheable
public class ValkeyCacheConfig {

    // Inject properties using @Value
    @Value("${valkey.host:localhost}")
    private String redisHost;

    @Value("${valkey.port:6379}")
    private int redisPort;

    @Value("${valkey.password:#{null}}")
    private String redisPassword;

    // Optional: Make SSL configurable
    // @Value("${valkey.ssl.enabled:false}")
    // private boolean redisSslEnabled;

    @Bean
    public RedisStandaloneConfiguration redisStandaloneConfiguration() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);

        // Configure password if provided in properties
        if (StringUtils.hasText(redisPassword)) {
            config.setPassword(redisPassword);
        }

        // Optionally configure database index if needed
        // config.setDatabase(databaseIndex);

        return config;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisStandaloneConfiguration standaloneConfig) {
        // Basic LettuceConnectionFactory setup
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig);

        // Configure SSL based on property (example)
        // if (redisSslEnabled) {
        //     factory.setUseSsl(true);
        //     // Potentially add further SSL configuration here if needed
        // } else {
        //     factory.setUseSsl(false);
        // }

        // Share native connection for performance
        factory.setShareNativeConnection(true);

        // Validate connection on startup (optional but recommended)
        factory.setValidateConnection(true);

        return factory;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        // Standard StringRedisTemplate for simple String key/value operations
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys (standard practice)
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Use JSON serializer for values (good for storing complex objects)
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // Enable transaction support if needed for atomic operations across multiple commands
        // template.setEnableTransactionSupport(true);

        template.afterPropertiesSet(); // Ensure serializers are initialized
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL for caches without a specific config
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
                )
                .disableCachingNullValues(); // Prevent caching null values

        // Specific configurations for different caches
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Product lists/searches - maybe longer TTL?
        cacheConfigurations.put(Constants.PRODUCTS_CACHE, defaultConfig.entryTtl(PRODUCTS_CACHE_TTL));
        // Category lists/searches
        cacheConfigurations.put(Constants.CATEGORIES_CACHE, defaultConfig.entryTtl(CATEGORIES_CACHE_TTL));
        // User details - shorter TTL?
        cacheConfigurations.put(Constants.USERS_CACHE, defaultConfig.entryTtl(USERS_CACHE_TTL));
        // Roles - relatively static, maybe longer TTL
        cacheConfigurations.put(Constants.ROLES_CACHE, defaultConfig.entryTtl(ROLES_CACHE_TTL));
        // ASN - very static, long TTL
        cacheConfigurations.put(Constants.ASN_CACHE, defaultConfig.entryTtl(ASN_CACHE_TTL));
        // Tokens - TTL should match token validity, often handled differently (e.g., direct Redis ops)
        // but if using @Cacheable, set a reasonable TTL like 1 hour or less.
        cacheConfigurations.put(Constants.TOKENS_CACHE, defaultConfig.entryTtl(TOKENS_CACHE_TTL));
        // Temporary cache - short TTL, maybe cleaned by maintenance anyway
        cacheConfigurations.put(Constants.TEMPORARY_CACHE, defaultConfig.entryTtl(TEMPORARY_CACHE_TTL));

        // Add configurations from Constants if they exist and need specific TTLs
        // cacheConfigurations.put(Constants.PRODUCT_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)));
        // cacheConfigurations.put(Constants.USERS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Build the cache manager with default and specific configurations
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig) // Apply default config to any cache not explicitly configured
                // Add specific configurations for named caches if needed
//                .withCacheConfiguration(USERS_CACHE,
//                        defaultConfig.entryTtl(Duration.ofMinutes(10))) // Override TTL for userCache
//                .withCacheConfiguration(PRODUCT_CACHE,
//                        defaultConfig.entryTtl(Duration.ofHours(1))) // Override TTL for productCache
                // Add more cache configurations as needed
                // .withCacheConfiguration("anotherCache", ...)
                .withInitialCacheConfigurations(cacheConfigurations) // Apply specific configurations
                .transactionAware() // Enable if you need cache operations to be aware of Spring transactions
                .build();
    }

}
