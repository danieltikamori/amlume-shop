/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.cache_management.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry; // Keep for custom counters if needed
    private final StringRedisTemplate redisTemplate; // Keep for direct Redis ops

    public CacheService(CacheManager cacheManager, MeterRegistry meterRegistry, StringRedisTemplate redisTemplate) {
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Gets an item from the cache or computes/caches it using the dataSupplier.
     * Note: Prefer using @Cacheable on service methods where possible.
     *
     * @param cacheName    Name of the cache.
     * @param key          Cache key.
     * @param dataSupplier Function to load data on cache miss.
     * @param <T>          Type of the cached item.
     * @return The cached or newly loaded item.
     */
    public <T> T getOrCache(String cacheName, String key, Supplier<T> dataSupplier) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            // Log error or handle differently if cache might legitimately not exist
            log.error("Cache not found during getOrCache: {}", cacheName);
            throw new IllegalStateException("Cache not found: " + cacheName);
        }

        // Use cache.get(key, Class<T>) to potentially avoid manual casting if underlying cache supports it
        // Or stick to ValueWrapper for broader compatibility
        Cache.ValueWrapper valueWrapper = cache.get(key);

        if (valueWrapper != null) {
            // Actuator automatically records hits/misses if configured
            log.trace("Cache hit for cache: {}, key: {}", cacheName, key); // Use trace level
            @SuppressWarnings("unchecked")
            T value = (T) valueWrapper.get();
            return value;
        } else {
            // Actuator automatically records hits/misses if configured
            log.debug("Cache miss for cache: {}, key: {}. Loading from supplier.", cacheName, key);
            T value = dataSupplier.get();
            // Consider null handling - Spring's default RedisCacheConfiguration disables caching nulls
            if (value != null) {
                cache.put(key, value);
            } else {
                log.warn("Supplier returned null for cache: {}, key: {}. Value not cached.", cacheName, key);
            }
            return value;
        }
    }

    public void invalidate(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            try {
                cache.evict(key);
                // Keep custom counter if you want specific 'manual' invalidation tracking
                meterRegistry.counter("cache.manual.invalidations", "cache", cacheName, "type", "single").increment();
                log.debug("Cache entry invalidated - cache: {}, key: {}", cacheName, key);
            } catch (Exception e) {
                // Log error for manual invalidation failure
                meterRegistry.counter("cache.manual.invalidations.errors", "cache", cacheName, "type", "single").increment();
                log.error("Failed to invalidate cache entry - cache: {}, key: {}", cacheName, key, e);
                // Decide if this should be a fatal exception
                // throw new CacheOperationException.CacheInvalidationException("Failed to invalidate cache entry", e);
            }
        } else {
            log.warn("Attempted to invalidate key '{}' in non-existent cache: {}", key, cacheName);
        }
    }

    public void invalidateAll(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            try {
                cache.clear();
                // Keep custom counter if you want specific 'manual' clear tracking
                meterRegistry.counter("cache.manual.invalidations", "cache", cacheName, "type", "all").increment();
                log.info("Cache cleared - cache: {}", cacheName); // Use info level for full clear
            } catch (Exception e) {
                // Log error for manual clear failure
                meterRegistry.counter("cache.manual.invalidations.errors", "cache", cacheName, "type", "all").increment();
                log.error("Failed to clear cache - cache: {}", cacheName, e);
                // Decide if this should be a fatal exception
                // throw new CacheOperationException.CacheClearException("Failed to clear cache", e);
            }
        } else {
            log.warn("Attempted to clear non-existent cache: {}", cacheName);
        }
    }

    // This method is fine as it's direct Redis interaction
    public void setWithExpiry(String key, String value, Duration expiry) {
        try {
            redisTemplate.opsForValue().set(key, value, expiry);
        } catch (Exception e) {
            log.error("Failed to set key '{}' in Redis with expiry {}: {}", key, expiry, e.getMessage(), e);
            // Handle exception as needed
        }
    }
}
