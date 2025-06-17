/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Configuration for Valkey (Redis fork) connection.
 * This class provides a custom RedisConnectionFactory with optimized settings
 * for connecting to Valkey with SSL/TLS support.
 */
@Configuration
public class ValkeyCacheConfig {

    private static final Logger log = LoggerFactory.getLogger(ValkeyCacheConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

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
        if (password != null && !password.isEmpty()) {
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
}
