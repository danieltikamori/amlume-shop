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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.config.properties.ValkeyConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;

import static me.amlu.shop.amlume_shop.commons.Constants.*;

/**
 * Configuration class for setting up caching and Redis/Valkey connection
 * using Spring Data Redis with Lettuce connector.
 * Configures RedisTemplate, StringRedisTemplate, and the primary CacheManager beans
 * using RedisCacheManagerBuilderCustomizer for better Spring Boot integration.
 */
@Configuration
@EnableCaching // Enables Spring's caching annotations like @Cacheable
public class ValkeyCacheConfig {

    private static final Logger log = LoggerFactory.getLogger(ValkeyCacheConfig.class); // Add logger

    // --- Cache Names for IP Security & Geolocation ---
    public static final String IP_BLOCK_CACHE = "ipBlockCache";
    public static final String IP_METADATA_CACHE = "ipMetadataCache";
    public static final String GEO_LOCATION_CACHE = "geoLocationCache";
    public static final String GEO_HISTORY_CACHE = "geoHistoryCache";

    @Value("${security.geo.time-window-hours:24}")
    private int geoHistoryTtlHours;


    // TLS connection properties
    // Inject central SSL properties
    @Value("${app.ssl.trust-store.path}")
    private String centralTruststorePath;

    @Value("${app.ssl.trust-store.password}")
    private String centralTruststorePassword;

    private final ResourceLoader resourceLoader; // Inject ResourceLoader

    // Inject properties using ValkeyConfigProperties bean instead of @Value
    private final ValkeyConfigProperties valkeyConfigProperties; // Inject the properties bean


    // Inject the application's pre-configured ObjectMapper
    private final ObjectMapper objectMapper;

    // Updated constructor to inject ValkeyConfigProperties
    public ValkeyCacheConfig(ResourceLoader resourceLoader, ObjectMapper objectMapper, ValkeyConfigProperties valkeyConfigProperties) {
        this.resourceLoader = resourceLoader;
        this.valkeyConfigProperties = valkeyConfigProperties; // Assign injected properties

        // Configure the ObjectMapper specifically for cache serialization needs
        this.objectMapper = objectMapper.copy(); // Work on a copy to avoid side effects
        this.objectMapper.registerModule(new JavaTimeModule()); // Ensure Java 8+ time types are handled
        // Default typing configuration remains commented out as before
    }

    /**
     * Configures Redis standalone connection details using ValkeyConfigProperties.
     * Must use Valkey password.
     *
     * @return configuration
     */
    @Bean
    public RedisStandaloneConfiguration redisStandaloneConfiguration() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        // TODO: Ensure ValkeyConfigProperties has getHost() and getPort() methods and is populated correctly (e.g., from Vault).
        // Currently, it has getNodes(). If getNodes() returns "host:port", parsing is needed here.
        // Assuming ValkeyConfigProperties provides individual host and port for simplicity now.
        // String host = parseHostFromNodes(valkeyConfigProperties.getNodes()); // Example parsing needed if using 'nodes'
        // int port = parsePortFromNodes(valkeyConfigProperties.getNodes()); // Example parsing needed

        // Using placeholder getters - replace with actual methods from ValkeyConfigProperties
        String host = valkeyConfigProperties.getHost(); // Assuming getHost() exists
        int port = valkeyConfigProperties.getPort();   // Assuming getPort() exists
        String password = valkeyConfigProperties.getPassword(); // Assuming getPassword() exists

        log.info("Configuring Valkey connection to host: {}, port: {}", host, port); // Log connection details
        config.setHostName(host); // Use host from properties
        config.setPort(port);     // Use port from properties

        if (StringUtils.hasText(password)) {
            config.setPassword(password); // Set password from properties
            log.info("Valkey password is set.");
        } else {
            log.error("CRITICAL: Valkey password is required but not configured in ValkeyConfigProperties!");
            throw new IllegalStateException("Valkey password is required but not configured!");
        }
        return config;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisStandaloneConfiguration standaloneConfig) {
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder = LettuceClientConfiguration.builder();

        // Check if Valkey SSL is enabled (using valkey.ssl.enabled property)
        if (valkeyConfigProperties.getSsl() != null && valkeyConfigProperties.getSsl().isEnabled()) {
            log.info("Enabling SSL/TLS for Valkey connection.");
            clientConfigBuilder.useSsl();

            // --- Configure Trust Strategy using CENTRAL Truststore ---
            if (StringUtils.hasText(centralTruststorePath) && StringUtils.hasText(centralTruststorePassword)) {
                try {
                    // Use ResourceLoader to resolve the path (handles file: and classpath:)
                    Resource truststoreResource = resourceLoader.getResource(centralTruststorePath);

                    if (!truststoreResource.exists()) {
                        log.error("Central truststore resource not found at: {}", centralTruststorePath);
                        throw new IllegalStateException("Central truststore resource not found for Valkey TLS: " + centralTruststorePath);
                    }

                    log.info("Configuring Valkey TLS using CENTRAL truststore: {}", centralTruststorePath);

                    SslOptions sslOptions = SslOptions.builder()
                            .truststore(truststoreResource, centralTruststorePassword.toCharArray()) // Use Resource
                            .build();

                    ClientOptions clientOptions = ClientOptions.builder()
                            .sslOptions(sslOptions)
                            .build();

                    clientConfigBuilder.clientOptions(clientOptions);
                    log.info("Successfully configured Lettuce client options with CENTRAL truststore.");

                } catch (IOException e) {
                    log.error("IOException while accessing central truststore resource: {}", centralTruststorePath, e);
                    throw new IllegalStateException("Failed to access central truststore resource for Valkey TLS", e);
                } catch (Exception e) {
                    log.error("Failed to configure Lettuce SSL options with central truststore: {}", centralTruststorePath, e);
                    throw new IllegalStateException("Failed to configure Lettuce SSL options with central truststore", e);
                }
            } else {
                log.warn("Central truststore path or password not configured (app.ssl.trust-store.*). " +
                        "Attempting to use system default trust anchors for Valkey TLS. " +
                        "This requires the Valkey server's certificate CA to be trusted by the JVM/OS.");
                // Rely on system defaults (no explicit SslOptions needed)
            }
        } else {
            log.info("SSL/TLS is disabled for Valkey connection.");
        }

        LettuceClientConfiguration clientConfiguration = clientConfigBuilder.build();
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig, clientConfiguration);
        factory.setShareNativeConnection(true);
        factory.setValidateConnection(true);
        return factory;
    }


    // Keep RedisTemplate beans if they are used directly elsewhere in the application
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        // Use the ObjectMapper configured in the constructor for consistency
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(this.objectMapper);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration() {
        // Define the default cache configuration (serializers, default TTL, null values)
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(this.objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL if not overridden
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer) // Use configured ObjectMapper
                )
                .disableCachingNullValues(); // Prevent caching null values
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
                .withCacheConfiguration(Constants.PRODUCTS_CACHE, defaultCacheConfig.entryTtl(PRODUCTS_CACHE_TTL))
                .withCacheConfiguration(Constants.CATEGORIES_CACHE, defaultCacheConfig.entryTtl(CATEGORIES_CACHE_TTL))
                .withCacheConfiguration(Constants.USERS_CACHE, defaultCacheConfig.entryTtl(USERS_CACHE_TTL))
                .withCacheConfiguration(Constants.ROLES_CACHE, defaultCacheConfig.entryTtl(ROLES_CACHE_TTL))
                .withCacheConfiguration(Constants.ASN_CACHE, defaultCacheConfig.entryTtl(ASN_CACHE_TTL))
                .withCacheConfiguration(Constants.TOKENS_CACHE, defaultCacheConfig.entryTtl(TOKENS_CACHE_TTL))
                .withCacheConfiguration(Constants.TEMPORARY_CACHE, defaultCacheConfig.entryTtl(TEMPORARY_CACHE_TTL))
                .withCacheConfiguration(Constants.HCP_SECRETS_CACHE, defaultCacheConfig.entryTtl(HCP_SECRETS_CACHE_TTL))
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
        return redisScript;
    }

    // Make sure the PostConstruct test uses the factory configured above
    @jakarta.annotation.PostConstruct
    public void testValkeyConnection() {
        try {
            // Get the factory bean which is now configured with central SSL
            LettuceConnectionFactory factory = redisConnectionFactory(redisStandaloneConfiguration());
            RedisTemplate<String, Object> template = redisTemplate(factory); // Use the configured factory
            assert template.getConnectionFactory() != null;
            String result = template.getConnectionFactory().getConnection().ping();
            if ("PONG".equalsIgnoreCase(result)) {
                log.info("Successfully connected to Valkey (using central SSL config) and received PONG.");
            } else {
                log.warn("Connected to Valkey (using central SSL config), but PING response was unexpected: {}", result);
            }
        } catch (Exception e) {
            log.error("!!! FAILED TO CONNECT TO VALKEY (using central SSL config) during PostConstruct test !!! Check SSL/TLS configuration, host, port, password, and network.", e);
            // throw new IllegalStateException("Failed to establish initial connection to Valkey", e);
        }
    }
}