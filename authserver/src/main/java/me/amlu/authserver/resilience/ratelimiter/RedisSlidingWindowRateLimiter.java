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
package me.amlu.authserver.resilience.ratelimiter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import me.amlu.authserver.exceptions.RateLimitException;
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

/**
 * {@code RedisSlidingWindowRateLimiter} implements a rate limiting mechanism using a Redis-backed sliding window algorithm.
 * This class leverages Redis's sorted sets to store timestamps of requests, allowing for efficient
 * counting of requests within a defined time window.
 *
 * <p>It integrates with Resilience4j for robust error handling, specifically using Circuit Breaker
 * and Retry patterns to manage potential Redis connectivity issues or transient failures.
 *
 * <p>The rate limiter can be configured with different limits and window durations for various
 * types of requests, identified by a "limiter name" extracted from the request key.
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * @Autowired
 * private RateLimiter rateLimiter;
 *
 * public void processRequest(String userId, String ipAddress) {
 *     if (rateLimiter.tryAcquire("login:" + ipAddress)) {
 *         // Proceed with request
 *     } else {
 *         throw new TooManyRequestsException("Rate limit exceeded for IP: " + ipAddress);
 *     }
 * }
 * }</pre>
 */
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

    /**
     * Constructs a new {@code RedisSlidingWindowRateLimiter}.
     *
     * @param stringRedisTemplate            The {@link StringRedisTemplate} for Redis operations.
     * @param slidingWindowRateLimiterScript The pre-loaded Redis Lua script for sliding window logic.
     * @param properties                     The {@link RateLimiterProperties} containing rate limiter configurations.
     * @param clock                          The {@link Clock} instance for obtaining current time, allowing for testability.
     * @param circuitBreakerRegistry         The {@link CircuitBreakerRegistry} to obtain or create Circuit Breaker instances.
     * @param retryRegistry                  The {@link RetryRegistry} to obtain or create Retry instances.
     * @throws NullPointerException if any of the required dependencies are null.
     */
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

    /**
     * Attempts to acquire a permit for the given key. This method applies the sliding window
     * rate limiting logic. It also incorporates Resilience4j's Circuit Breaker and Retry
     * patterns to handle transient Redis failures.
     *
     * @param key The key representing the resource or client to be rate-limited (e.g., "login:192.168.1.1").
     * @return {@code true} if a permit was successfully acquired (request is allowed), {@code false} otherwise.
     * @throws RateLimitException if a non-transient error occurs during the rate limiting check and {@code failOpen} is false.
     */
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

    /**
     * Executes the Redis Lua script to perform the sliding window rate limiting check.
     * This method is decorated by Resilience4j components in {@link #tryAcquire(String)}.
     *
     * @param limiterName The name of the rate limiter configuration to use (e.g., "login", "captcha").
     * @param identifier  The specific identifier for the rate-limited entity (e.g., "192.168.1.1", "user123").
     * @param config      The {@link RateLimiterProperties.LimiterConfig} for the current limiter.
     * @return {@code true} if the request is allowed by the Redis script, {@code false} otherwise.
     * @throws RateLimitException if a Redis-related error occurs during script execution.
     */
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

    /**
     * Calculates an approximate number of remaining permits for a given key.
     * This method queries Redis to count the number of events within the current sliding window
     * and subtracts it from the configured limit.
     *
     * <p>Note: This is an approximation. A precise remaining count in a true sliding window
     * is complex and often not strictly necessary for practical rate limiting.
     *
     * @param key The key for which to get remaining permits.
     * @return The approximate number of remaining permits, or -1 if an error occurs.
     */
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
     * Assumes the key format is "{@code limiterName:identifier}".
     * If the key does not contain a colon (':') or the colon is at the beginning/end,
     * it logs a warning and returns "default" as the limiter name.
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

    /**
     * Extracts the identifier from the key.
     * Assumes the key format is "{@code limiterName:identifier}".
     * If the key does not contain a colon (':') or the colon is at the beginning/end,
     * it logs a warning and returns the full key as the identifier.
     *
     * @param key The key to extract the identifier from.
     * @return The extracted identifier or the full key if no valid separator is found.
     */
    private String extractIdentifier(String key) {
        int separatorIndex = key.indexOf(':');
        if (separatorIndex > 0 && separatorIndex < key.length() - 1) {
            return key.substring(separatorIndex + 1);
        }
        log.warn("Could not extract identifier from key '{}'. Using full key.", key);
        return key; // Use the full key if no separator
    }
}
