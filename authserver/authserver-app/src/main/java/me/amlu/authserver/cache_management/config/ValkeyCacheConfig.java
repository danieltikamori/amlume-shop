/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.cache_management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import me.amlu.authserver.cache_management.config.properties.ValkeyConfigProperties;
import me.amlu.authserver.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

import static me.amlu.authserver.common.CacheKeys.*;
import static me.amlu.authserver.common.SecurityConstants.*;

/**
 * Configuration for Valkey (Redis fork) connection.
 * This class provides a custom RedisConnectionFactory with optimized settings
 * for connecting to Valkey with SSL/TLS support.
 */
@Configuration
@EnableCaching
public class ValkeyCacheConfig {

    private static final Logger log = LoggerFactory.getLogger(ValkeyCacheConfig.class);

    @Value("${security.geo.time-window-hours:24}")
    private int geoHistoryTtlHours;

    // TLS connection properties
    // Inject central SSL properties
    @Value("${app.ssl.trust-store.path}")
    private String centralTruststorePath;

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    private final ResourceLoader resourceLoader;

    // --- Inject properties using ValkeyConfigProperties bean instead of @Value
    private final ValkeyConfigProperties valkeyConfigProperties;
    private final ObjectMapper objectMapper;

    // --- Field to store the connection factory ---
    private final RedisConnectionFactory connectionFactory;

    // Updated constructor to inject ValkeyConfigProperties AND RedisConnectionFactory
    public ValkeyCacheConfig(ObjectMapper objectMapper, ResourceLoader resourceLoader,
                             ValkeyConfigProperties valkeyConfigProperties,
                             @Lazy RedisConnectionFactory connectionFactory
    ) {
        this.resourceLoader = resourceLoader;
        this.valkeyConfigProperties = valkeyConfigProperties; // Assign injected properties
        // --- ADDED: Assign injected factory ---
        this.connectionFactory = connectionFactory;
        this.objectMapper = objectMapper.copy();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Creates a custom LettuceConnectionFactory with optimized settings.
     * This factory is configured with SSL/TLS support, connection timeouts,
     * and other performance settings.
     *
     * @return A configured LettuceConnectionFactory
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        // This customizer is now the primary way to configure caches.
        // Spring Boot auto-configuration will pick it up and create the CacheManager.
        return (builder) -> {
            // Default configuration for all caches
            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(30))
                    .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(this.objectMapper)))
                    .disableCachingNullValues();

            builder.withCacheConfiguration(USER_CACHE, defaultConfig.entryTtl(USERS_CACHE_TTL))
                    .withCacheConfiguration(ROLES_CACHE, defaultConfig.entryTtl(ROLES_CACHE_TTL))
                    .withCacheConfiguration(TOKEN_CACHE, defaultConfig.entryTtl(TOKENS_CACHE_TTL))
                    .withCacheConfiguration(SESSION_CACHE, defaultConfig.entryTtl(SESSIONS_CACHE_TTL))
                    .withCacheConfiguration(PASSKEYS_CACHE, defaultConfig.entryTtl(PASSKEYS_CACHE_TTL))
                    .withCacheConfiguration(CLIENTS_CACHE, defaultConfig.entryTtl(CLIENTS_CACHE_TTL))
                    .withCacheConfiguration(SECURITY_EVENTS_CACHE, defaultConfig.entryTtl(SECURITY_EVENTS_CACHE_TTL))
                    .withCacheConfiguration(IP_METADATA_CACHE, defaultConfig.entryTtl(IP_METADATA_CACHE_TTL))
                    .withCacheConfiguration(ASN_CACHE, defaultConfig.entryTtl(ASN_CACHE_TTL))
                    .withCacheConfiguration(TEMPORARY_CACHE, defaultConfig.entryTtl(TEMPORARY_CACHE_TTL))
                    .transactionAware();
        };
    }

    // This bean is still useful if you perform direct Redis operations elsewhere.
    // Spring correctly injects the auto-configured RedisConnectionFactory here as a method parameter.
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        log.debug("Bean StringRedisTemplate initialized with connection factory.");
        return template;
    }

    /**
     * Creates a RedisTemplate with appropriate serializers.
     * <p>
     * Configures a RedisTemplate with:
     * <ul>
     *   <li>String serializer for keys</li>
     *   <li>JSON serializer for values</li>
     *   <li>String serializer for hash keys</li>
     *   <li>JSON serializer for hash values</li>
     * </ul>
     * This configuration ensures proper serialization/deserialization of
     * complex objects to/from Redis.
     * </p>
     *
     * @param connectionFactory The Redis connection factory to use
     * @return A configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(this.objectMapper);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        log.debug("Bean RedisTemplate initialized with custom serializers.");
        return template;
    }

    // Keep Lua script bean if used for rate limiting or other custom Redis operations
    @Bean
    public RedisScript<Long> slidingWindowRateLimiterScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("scripts/sliding_window_rate_limit_check_first.lua"));
        redisScript.setResultType(Long.class);
        log.debug("Bean SlidingWindowRateLimiterScript initialized for sliding window rate limiting.");
        return redisScript;
    }

    public void testValkeyConnection() {
        try {
            String result = this.connectionFactory.getConnection().ping();

            if ("PONG".equalsIgnoreCase(result)) {
                log.info("Successfully connected to Valkey (using central SSL config) and received PONG.");
            } else {
                log.warn("Connected to Valkey (using central SSL config), but PING response was unexpected: {}", result);
            }
        } catch (Exception e) {
            log.error("!!! FAILED TO CONNECT TO VALKEY (using central SSL config) during PostConstruct test !!! Check SSL/TLS configuration, host, port, password, and network.", e);
            // Consider re-throwing or handling based on whether startup should fail
            // throw new IllegalStateException("Failed to establish initial connection to Valkey", e);
        }
    }

    // --- Optional: Alternative using ApplicationReadyEvent ---
    /*
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void testValkeyConnectionOnReady() {
        log.info("Application ready. Performing Valkey connection test...");
        try {
            String result = this.connectionFactory.getConnection().ping();
            if ("PONG".equalsIgnoreCase(result)) {
                log.info("Successfully connected to Valkey (post-startup) and received PONG.");
            } else {
                log.warn("Connected to Valkey (post-startup), but PING response was unexpected: {}", result);
            }
        } catch (Exception e) {
            log.error("!!! FAILED TO CONNECT TO VALKEY (post-startup) !!! Check configuration and network.", e);
            // Consider logging or metrics, but don't block application shutdown usually
        }
    }
    */
}
