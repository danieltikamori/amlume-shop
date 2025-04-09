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

/**
 * Configuration class for setting up caching and Redis/Valkey connection
 * using Spring Data Redis with Lettuce connector.
 * Configures RedisTemplate, StringRedisTemplate, and CacheManager beans.
 */
@Configuration
@EnableCaching // Enables Spring's caching annotations like @Cacheable
public class ValkeyCacheConfig {

    // Inject properties using @Value
    @Value("${valkey.host:localhost}") // Default to localhost if property not set
    private String redisHost;

    @Value("${valkey.port:6379}") // Default to 6379
    private int redisPort;

    @Value("${valkey.password:#{null}}") // Default to null if property not set
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
                .entryTtl(Duration.ofMinutes(30)) // Default TTL for caches without specific config
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
                )
                .disableCachingNullValues(); // Prevent caching null values

        // Build the cache manager with default and specific configurations
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // Add specific configurations for named caches if needed
                .withCacheConfiguration("userCache",
                        defaultConfig.entryTtl(Duration.ofMinutes(10))) // Override TTL for userCache
                .withCacheConfiguration("productCache",
                        defaultConfig.entryTtl(Duration.ofHours(1))) // Override TTL for productCache
                // Add more cache configurations as needed
                // .withCacheConfiguration("anotherCache", ...)
                .build();
    }

}
