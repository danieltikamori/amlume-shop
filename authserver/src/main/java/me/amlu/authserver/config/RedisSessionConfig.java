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

// ObjectMapper import might not be needed here if springSessionDefaultRedisSerializer is defined in SessionConfig

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisSessionConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisSessionConfig.class);

    // This bean is fine if you need a RedisTemplate for general use.
    // It correctly depends on a `springSessionDefaultRedisSerializer` bean,
    // which will be provided by SessionConfig.
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            LettuceConnectionFactory lettuceConnectionFactory,
            RedisSerializer<Object> springSessionDefaultRedisSerializer) { // This will be injected from SessionConfig
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(lettuceConnectionFactory);
        template.setDefaultSerializer(springSessionDefaultRedisSerializer); // Use the Jackson serializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        // Value and HashValue serializers will use the default (springSessionDefaultRedisSerializer)
        log.info("Configured RedisTemplate with Jackson-based default serializer.");
        return template;
    }
}

