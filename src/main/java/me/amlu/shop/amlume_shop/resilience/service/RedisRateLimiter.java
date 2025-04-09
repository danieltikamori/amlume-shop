/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.resilience.service;

import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.security.paseto.TokenConfigurationService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisRateLimiter {

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<Boolean> rateLimiterScript;
//    private final StringRedisTemplate stringRedisTemplate;
    private final int windowSizeInSeconds;
    private final int maxRequestsPerWindow;

    public RedisRateLimiter(RedisTemplate<String, String> redisTemplate, TokenConfigurationService tokenConfigurationService) {
        this.redisTemplate = redisTemplate;
        this.windowSizeInSeconds = tokenConfigurationService.getValidationRateLimitWindowSizeInSeconds();
        this.maxRequestsPerWindow = (int) tokenConfigurationService.getValidationRateLimitPermitsPerSecond();
        this.rateLimiterScript = new DefaultRedisScript<>();
        rateLimiterScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("rate_limiter.lua")));
        rateLimiterScript.setResultType(Boolean.class);
    }

    public boolean tryAcquire(String key) {
        long currentTime = Instant.now().toEpochMilli();
        String redisKey = "rate-limit:" + key;

        List<String> keys = List.of(redisKey);
        List<String> args = List.of(String.valueOf(currentTime), String.valueOf(windowSizeInSeconds * 1000L), String.valueOf(maxRequestsPerWindow));

        Boolean result = redisTemplate.execute(rateLimiterScript, keys, args.toArray());
        return result != null && result;
    }
}

//    public boolean tryAcquire(String key) {
//        String redisKey = "ratelimit:" + key;
//
//        return redisTemplate.execute(new SessionCallback<Boolean>() {
//            @Override
//            public Boolean execute(RedisOperations operations) throws DataAccessException {
//                operations.multi();
//
//                operations.opsForValue().increment(redisKey, 1);
//                operations.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
//
//                List<Object> results = operations.exec();
//                Long count = (Long) results.get(0);
//
//                return count <= maxRequests;
//            }
//        });
//    }

