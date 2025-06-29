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

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuration for Redis connection and template with resilient settings.
 * <p>
 * This class configures Redis connections with appropriate timeouts and
 * serializers to ensure resilient caching operations. It uses the Lettuce
 * client with command timeouts to prevent hanging operations.
 * </p>
 */

//@Configuration
public class RedisConfig {

    /**
     * Creates a Redis connection factory with timeout settings.
     * <p>
     * Configures a Lettuce-based Redis connection with:
     * <ul>
     *   <li>Command timeout: 2 seconds</li>
     *   <li>Shutdown timeout: 1 second</li>
     * </ul>
     * These timeouts prevent Redis operations from blocking indefinitely
     * when the Redis server is slow or unresponsive.
     * </p>
     *
     * @param redisProperties The Redis properties from application configuration
     * @return A configured RedisConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(
                redisProperties.getHost(), redisProperties.getPort());

        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            redisConfig.setPassword(redisProperties.getPassword());
        }

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))
                .shutdownTimeout(Duration.ofSeconds(1))
                .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
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
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//        template.setHashKeySerializer(new StringRedisSerializer());
//        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
//        template.afterPropertiesSet();
//        return template;
//    }
}
