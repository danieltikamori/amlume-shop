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

    // Inject properties using ValkeyConfigProperties bean instead of @Value
    private final ValkeyConfigProperties valkeyConfigProperties; // Inject the properties bean

    // Removed @Value injections for host, port, password
    // private String redisHost;
    // private int redisPort;
    // private String redisPassword;

    @Value("${security.geo.time-window-hours:24}")
    private int geoHistoryTtlHours;

    // Inject the application's pre-configured ObjectMapper
    private final ObjectMapper objectMapper;

    // Updated constructor to inject ValkeyConfigProperties
    public ValkeyCacheConfig(ObjectMapper objectMapper, ValkeyConfigProperties valkeyConfigProperties) {
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

        // 1. Enable SSL/TLS
        log.info("Enabling SSL/TLS for Valkey connection.");
        clientConfigBuilder.useSsl();

        // 2. Configure Trust Strategy (Choose ONE for Production)

        // --- REMOVED Option A: Disable Peer Verification (INSECURE) ---
        log.warn("REMOVED InsecureTrustManagerFactory. Production requires a secure trust strategy (Truststore or System CA).");
        /*
        clientConfigBuilder.clientOptions(ClientOptions.builder()
                .sslOptions(SslOptions.builder()
                        .sslContext(sslCustomizer -> {
                            try {
                                sslCustomizer.trustManager(InsecureTrustManagerFactory.INSTANCE);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to configure Lettuce SSL context with insecure trust manager", e);
                            }
                        })
                        .build())
                .build());
        */
        // --- End REMOVED Option A ---

        // --- Option B: Use a Truststore (SECURE - Recommended for Production) ---
        // TODO: Uncomment and configure this section for production environments.
        // Requires creating a JKS truststore containing the server's CA or self-signed cert.
        // Example using keytool:
        // keytool -importcert -file ./certificates/valkey.crt -alias valkey-dev -keystore truststore.jks -storepass your_truststore_password -noprompt
        /*
        try {
            // TODO: Externalize truststore path and password (e.g., via Vault/Config Properties)
            File truststoreFile = new File("./config/truststore.jks"); // Adjust path as needed
            String truststorePassword = "your_truststore_password"; // GET FROM SECURE CONFIG

            if (!truststoreFile.exists()) {
                 log.error("Truststore file not found at: {}", truststoreFile.getAbsolutePath());
                 throw new IllegalStateException("Truststore file not found for Valkey TLS configuration.");
            }
            log.info("Configuring Valkey TLS using truststore: {}", truststoreFile.getAbsolutePath());

            SslOptions sslOptionsB = SslOptions.builder()
                    .truststore(truststoreFile, truststorePassword) // Use the correct method
                    .build();

            ClientOptions clientOptionsB = ClientOptions.builder()
                    .sslOptions(sslOptionsB)
                    .build();

            clientConfigBuilder.clientOptions(clientOptionsB);

        } catch (Exception e) {
            log.error("Failed to configure Lettuce SSL options with truststore", e);
            throw new IllegalStateException("Failed to configure Lettuce SSL options with truststore", e);
        }
        */
        // --- End Option B ---

        // --- Option C: Use System Trust Anchors (SECURE - If server cert is from a trusted CA) ---
        // If the Valkey server's certificate is signed by a CA already trusted by the JVM/OS,
        // Lettuce might pick it up automatically without explicit truststore config.
        // No specific SslOptions needed here, just ensure the CA is in the system truststore.
        // log.info("Configuring Valkey TLS using system default trust anchors.");
        // --- End Option C ---

        // TODO: Ensure ONE secure trust strategy (Option B or C) is configured for production.
        // If no trust strategy is configured after removing Option A, connection will likely fail
        // unless the server cert is trusted by the system AND Lettuce defaults work.

        // Build the final client configuration
        LettuceClientConfiguration clientConfiguration = clientConfigBuilder.build();

        // Create the factory using standalone config and client config
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig, clientConfiguration);
        factory.setShareNativeConnection(true); // Usually recommended
        factory.setValidateConnection(true); // Validate connections on checkout
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
}