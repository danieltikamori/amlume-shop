/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A resilient caching service that uses Redis with local fallback.
 * <p>
 * This service provides caching operations with circuit breaker and retry patterns
 * to handle Redis failures gracefully. When Redis is unavailable, it falls back
 * to an in-memory cache to maintain functionality.
 * </p>
 */

@Service
public class ResilientCacheService {
    private static final Logger log = LoggerFactory.getLogger(ResilientCacheService.class);
    private static final String CACHE_SERVICE = "cacheService";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, Object> localCache = new ConcurrentHashMap<>();

    public ResilientCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Retrieves a value from Redis with fallback to local cache.
     * <p>
     * Uses circuit breaker and retry patterns to handle Redis failures.
     * If Redis is unavailable, falls back to the local cache.
     * </p>
     *
     * @param key The key to retrieve
     * @return The cached value, or null if not found in either cache
     */
    @CircuitBreaker(name = CACHE_SERVICE, fallbackMethod = "getFromLocalCache")
    @Retry(name = CACHE_SERVICE)
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                // Update local cache as backup
                localCache.put(key, value);
            }
            return value;
        } catch (Exception e) {
            log.warn("Failed to get value from Redis for key: {}", key, e);
            return getFromLocalCache(key, e);
        }
    }

    /**
     * Stores a value in Redis with fallback to local cache.
     * <p>
     * Uses circuit breaker and retry patterns to handle Redis failures.
     * Always updates the local cache as a backup.
     * </p>
     *
     * @param key   The key to store
     * @param value The value to store
     * @param ttl   The time-to-live duration
     */
    @CircuitBreaker(name = CACHE_SERVICE, fallbackMethod = "putInLocalCache")
    @Retry(name = CACHE_SERVICE)
    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            // Update local cache as backup
            localCache.put(key, value);
        } catch (Exception e) {
            log.warn("Failed to put value in Redis for key: {}", key, e);
            putInLocalCache(key, value, ttl, e);
        }
    }

    /**
     * Gets a value from cache or computes it if not present.
     * <p>
     * This method implements the cache-aside pattern with resilience:
     * <ol>
     *   <li>Try to get the value from Redis</li>
     *   <li>If not found, compute the value using the supplier</li>
     *   <li>Store the computed value in Redis</li>
     *   <li>If Redis fails, fall back to local cache or compute</li>
     * </ol>
     * </p>
     *
     * @param <T>           The type of the cached value
     * @param key           The cache key
     * @param ttl           The time-to-live duration
     * @param valueSupplier A supplier function to compute the value if not in cache
     * @return The cached or computed value
     */
    @CircuitBreaker(name = CACHE_SERVICE)
    @Retry(name = CACHE_SERVICE)
    public <T> T getWithCompute(String key, Duration ttl, Supplier<T> valueSupplier) {
        try {
            @SuppressWarnings("unchecked")
            T value = (T) redisTemplate.opsForValue().get(key);

            if (value == null) {
                value = valueSupplier.get();
                if (value != null) {
                    redisTemplate.opsForValue().set(key, value, ttl);
                    localCache.put(key, value);
                }
            } else {
                localCache.put(key, value);
            }

            return value;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed for key: {}, using local cache or computing value", key);

            @SuppressWarnings("unchecked")
            T cachedValue = (T) localCache.get(key);
            if (cachedValue != null) {
                return cachedValue;
            }

            T computedValue = valueSupplier.get();
            if (computedValue != null) {
                localCache.put(key, computedValue);
            }
            return computedValue;
        }
    }

    // Fallback methods
    private Object getFromLocalCache(String key, Exception e) {
        log.debug("Falling back to local cache for key: {}", key);
        return localCache.get(key);
    }

    private void putInLocalCache(String key, Object value, Duration ttl, Exception e) {
        log.debug("Falling back to local cache for put operation, key: {}", key);
        localCache.put(key, value);
    }
}
