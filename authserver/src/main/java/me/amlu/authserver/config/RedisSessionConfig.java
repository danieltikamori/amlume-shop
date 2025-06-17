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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
//import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Configuration for Redis templates and connection factory.
 * This class provides beans for Redis operations with appropriate serializers.
 */
//@Configuration
//@EnableRedisHttpSession(redisNamespace = "authserver:session:amlume")
public class RedisSessionConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisSessionConfig.class);
    private final ObjectMapper primaryObjectMapper; // Inject primary ObjectMapper

    public RedisSessionConfig(ObjectMapper primaryObjectMapper) {
        this.primaryObjectMapper = primaryObjectMapper;
    }

    /**
     * Creates a RedisTemplate for general use with appropriate serializers.
     *
     * @param lettuceConnectionFactory The Redis connection factory
     * @return A configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            LettuceConnectionFactory lettuceConnectionFactory) { // Removed springSessionDefaultRedisSerializer
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(lettuceConnectionFactory);

        // Use the primary ObjectMapper for general Redis serialization
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer =
                new GenericJackson2JsonRedisSerializer(this.primaryObjectMapper);

        template.setDefaultSerializer(jackson2JsonRedisSerializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer); // Be explicit
        template.setHashValueSerializer(jackson2JsonRedisSerializer); // Be explicit

        log.info("Configured RedisTemplate with Jackson-based default serializer using primaryObjectMapper.");
        return template;
    }

    /**
     * Creates a specialized RedisTemplate for WebAuthn objects.
     * This template uses the WebAuthn-specific serializer for better handling
     * of WebAuthn objects in Redis.
     *
     * @param lettuceConnectionFactory The Redis connection factory
     * @param webAuthnRedisSerializer  The WebAuthn-specific serializer
     * @return A configured RedisTemplate for WebAuthn objects
     */
    @Bean
    @Qualifier("webAuthnRedisTemplate")
    public RedisTemplate<String, Object> webAuthnRedisTemplate(
            LettuceConnectionFactory lettuceConnectionFactory,
            @Qualifier("webAuthnRedisSerializer") RedisSerializer<Object> webAuthnRedisSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(lettuceConnectionFactory);
        template.setDefaultSerializer(webAuthnRedisSerializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(webAuthnRedisSerializer);
        template.setHashValueSerializer(webAuthnRedisSerializer);
        log.info("Configured specialized WebAuthnRedisTemplate with WebAuthn-specific serializer.");
        return template;
    }
}
