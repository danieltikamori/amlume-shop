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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

import static me.amlu.authserver.common.CacheKeys.*;
import static me.amlu.authserver.common.SecurityConstants.*;

/**
 * Configuration for Valkey (Redis fork) connection.
 * This class provides a custom RedisConnectionFactory with optimized settings
 * for connecting to Valkey with SSL/TLS support.
 */
@Configuration
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

    // --- Field to store the connection factory ---
    private final RedisConnectionFactory connectionFactory;

    // Updated constructor to inject ValkeyConfigProperties AND RedisConnectionFactory
    public ValkeyCacheConfig(ResourceLoader resourceLoader,
                             ValkeyConfigProperties valkeyConfigProperties,
                             @Lazy RedisConnectionFactory connectionFactory
    ) {
        this.resourceLoader = resourceLoader;
        this.valkeyConfigProperties = valkeyConfigProperties; // Assign injected properties
        // --- ADDED: Assign injected factory ---
        this.connectionFactory = connectionFactory;
    }

    /**
     * Creates a custom LettuceConnectionFactory with optimized settings.
     * This factory is configured with SSL/TLS support, connection timeouts,
     * and other performance settings.
     *
     * @param redisProperties The Redis properties from Spring Boot
     * @return A configured LettuceConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        // Configure Redis connection
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(host);
        redisConfig.setPort(port);

        // Set password if provided
        if (StringUtils.isNotBlank(password)) {
            redisConfig.setPassword(password);
        }

        // Build Lettuce client configuration with SSL if enabled
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
                LettuceClientConfiguration.builder();

        // Configure SSL if enabled
        if (sslEnabled) {
            builder.useSsl();
            log.info("SSL enabled for Redis/Valkey connection");
        }

        // Configure socket options
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // Configure client options
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(10)))
                .build();

        builder.clientOptions(clientOptions);

        // Set command timeout
        builder.commandTimeout(Duration.ofSeconds(5));

        // Create the connection factory
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                redisConfig, builder.build());

        log.info("Configured Redis/Valkey connection to {}:{} with SSL={}",
                host, port, sslEnabled);

        return factory;
    }

    // Keep RedisTemplate beans if they are used directly elsewhere in the application
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        log.debug("Bean StringRedisTemplate initialized with connection factory.");
        return template;
    }

    /**
     * Configures Spring's cache manager to use our custom Jackson serializer.
     * This will apply to any @Cacheable, @CachePut, etc., annotations.
     *
     * @param objectMapper The pre-configured ObjectMapper bean.
     * @return The Redis cache configuration.
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration(ObjectMapper objectMapper) {
        // Create a Jackson serializer with our custom ObjectMapper
        GenericJackson2JsonRedisSerializer jacksonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                // Set a reasonable time-to-live for cache entries
                .entryTtl(Duration.ofMinutes(60))
                // Disable caching of null values
                .disableCachingNullValues()
                // Define the serializer for cache values
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jacksonSerializer));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            RedisCacheConfiguration defaultCacheConfig) {
        // Use RedisCacheManagerBuilderCustomizer for better integration and flexibility
        return (builder) -> builder
                // Apply default config first
                .cacheDefaults(defaultCacheConfig)
                // --- Configure Specific Caches ---
                .withCacheConfiguration(IP_BLOCK_CACHE, defaultCacheConfig.entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration(IP_METADATA_CACHE, defaultCacheConfig.entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration(GEO_LOCATION_CACHE, defaultCacheConfig.entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration(GEO_HISTORY_CACHE, defaultCacheConfig.entryTtl(Duration.ofHours(this.geoHistoryTtlHours)))
                .withCacheConfiguration(USER_CACHE, defaultCacheConfig.entryTtl(USERS_CACHE_TTL))
                .withCacheConfiguration(ROLES_CACHE, defaultCacheConfig.entryTtl(ROLES_CACHE_TTL))
                .withCacheConfiguration(ASN_CACHE, defaultCacheConfig.entryTtl(ASN_CACHE_TTL))
                .withCacheConfiguration(TEMPORARY_CACHE, defaultCacheConfig.entryTtl(TEMPORARY_CACHE_TTL))
                .withCacheConfiguration(HCP_SECRETS_CACHE, defaultCacheConfig.entryTtl(HCP_SECRETS_CACHE_TTL))
                // --- Optional Features ---
                .enableStatistics()
                .transactionAware();
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
