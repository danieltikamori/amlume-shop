/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

// package me.amlu.shop.amlume_shop.resilience.service; // Or ratelimiter package
package me.amlu.shop.amlume_shop.resilience.ratelimiter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import me.amlu.shop.amlume_shop.exceptions.RateLimitException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@Service
public class RedisSlidingWindowRateLimiter implements RateLimiter {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(RedisSlidingWindowRateLimiter.class);
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> slidingWindowScript;
    private final RateLimiterProperties properties;
    private final Clock clock;
    private final CircuitBreaker redisCircuitBreaker;
    private final Retry redisRetry;

    // Define names for Resilience4j instances specific to this service
    private static final String REDIS_CB_NAME = "rateLimiterRedis";
    private static final String REDIS_RETRY_NAME = "rateLimiterRedis";

    public RedisSlidingWindowRateLimiter(
            StringRedisTemplate stringRedisTemplate,
            RedisScript<Long> slidingWindowRateLimiterScript, // Inject the script bean
            RateLimiterProperties properties,
            Clock clock,
            @Qualifier("circuitBreakerRegistry") CircuitBreakerRegistry circuitBreakerRegistry,
            @Qualifier("retryRegistry") RetryRegistry retryRegistry) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.slidingWindowScript = slidingWindowRateLimiterScript;
        this.properties = properties;
        this.clock = clock;
        // Get or create specific instances for this service
        this.redisCircuitBreaker = circuitBreakerRegistry.circuitBreaker(REDIS_CB_NAME);
        this.redisRetry = retryRegistry.retry(REDIS_RETRY_NAME);
    }

    @Override
    public boolean tryAcquire(String key) {
        // Determine which config to use based on the key or a context
        // For simplicity, let's assume the key itself implies the limiter name
        // e.g., key = "captcha:1.2.3.4", limiterName = "captcha"
        // Or, the caller needs to provide the limiter name.
        // Let's modify the interface slightly or use a convention.
        // Convention: key format "limiterName:identifier"
        String limiterName = extractLimiterName(key);
        String identifier = extractIdentifier(key);
        RateLimiterProperties.LimiterConfig config = properties.getConfigFor(limiterName);

        Supplier<Boolean> redisCallSupplier = () -> executeLuaScript(limiterName, identifier, config);

        // Decorate with Retry and Circuit Breaker
        Supplier<Boolean> resilientSupplier = Retry.decorateSupplier(redisRetry,
                CircuitBreaker.decorateSupplier(redisCircuitBreaker, redisCallSupplier)
        );

        try {
            return resilientSupplier.get();
        } catch (Exception e) {
            log.error("Rate limiting check failed for key '{}' after retries or due to circuit breaker.", key, e);
            // Handle failure based on failOpen property
            if (properties.isFailOpen()) {
                log.warn("Failing open for rate limit check on key '{}'", key);
                return true; // Allow request
            } else {
                // Re-throw a specific runtime exception
                throw new RateLimitException("Rate limiter backend unavailable for key: " + key, e);
            }
        }
    }

    private boolean executeLuaScript(String limiterName, String identifier, RateLimiterProperties.LimiterConfig config) {
        String redisKey = properties.getRedisKeyPrefix() + limiterName + ":" + identifier;
        long nowMillis = clock.instant().toEpochMilli();
        long windowMillis = config.getWindowDuration().toMillis();
        long limit = config.getLimit();

        List<String> keys = Collections.singletonList(redisKey);
        Object[] args = {
                String.valueOf(windowMillis),
                String.valueOf(limit),
                String.valueOf(nowMillis)
        };

        try {
            Long result = stringRedisTemplate.execute(slidingWindowScript, keys, args);
            boolean allowed = result == 1;
            log.trace("Rate limit check for key '{}': Allowed={}", redisKey, allowed);
            return allowed;
        } catch (DataAccessException e) {
            log.error("Redis error during rate limit check for key: {}", redisKey, e);
            // Let Resilience4j handle this exception
            throw new RateLimitException("Redis error during rate limit check for key: " + redisKey, e);
        } catch (Exception e) {
            log.error("Unexpected error executing Lua script for key: {}", redisKey, e);
            throw new RateLimitException("Unexpected error during rate limit check for key: " + redisKey, e);
        }
    }

    @Override
    public long getRemainingPermits(String key) {
        // Similar logic to tryAcquire to get config
        String limiterName = extractLimiterName(key);
        String identifier = extractIdentifier(key);
        RateLimiterProperties.LimiterConfig config = properties.getConfigFor(limiterName);
        String redisKey = properties.getRedisKeyPrefix() + limiterName + ":" + identifier;

        // Note: This is an approximation based on current count vs. limit.
        // A precise sliding window remaining count is complex.
        try {
            long nowMillis = clock.instant().toEpochMilli();
            long windowStartMillis = nowMillis - config.getWindowDuration().toMillis();

            // Count elements within the current window
            Long currentCount = stringRedisTemplate.opsForZSet().count(redisKey, windowStartMillis, nowMillis);
            long count = (currentCount != null) ? currentCount : 0;
            long remaining = Math.max(0, config.getLimit() - count);
            log.trace("Approximate remaining permits for key '{}': {}", redisKey, remaining);
            return remaining;
        } catch (Exception e) {
            log.error("Error getting remaining permits for key '{}'", redisKey, e);
            return -1; // Indicate error or inability to calculate
        }
    }

    // --- Helper methods for key parsing ---

    /**
     * Extracts the limiter name from the key.
     * IMPORTANT: Assumes the format "limiterName:identifier".
     * If no separator is found, returns "default".
     *
     * @param key The key to extract the limiter name from.
     * @return The extracted limiter name or "default" if not found.
     */
    private String extractLimiterName(String key) {
        int separatorIndex = key.indexOf(':');
        if (separatorIndex > 0) {
            return key.substring(0, separatorIndex);
        }
        log.warn("Could not extract limiter name from key '{}'. Using default.", key);
        return "default"; // Or throw an error if a format is strict
    }

    private String extractIdentifier(String key) {
        int separatorIndex = key.indexOf(':');
        if (separatorIndex > 0 && separatorIndex < key.length() - 1) {
            return key.substring(separatorIndex + 1);
        }
        log.warn("Could not extract identifier from key '{}'. Using full key.", key);
        return key; // Use the full key if no separator
    }
}