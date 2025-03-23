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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import me.amlu.shop.amlume_shop.commons.Constants;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static me.amlu.shop.amlume_shop.commons.Constants.AUTH_CACHE;

/**
 * This will give you basic cache metrics that you can monitor in Grafana. The metrics will be available under the names:
 * cache.size
 * cache.hits
 * cache.misses
 */

@Component
public class CacheMetrics {

    private final ConcurrentHashMap<String, AtomicLong> cacheHits;
    private final ConcurrentHashMap<String, AtomicLong> cacheMisses;

    private final MeterRegistry registry;
    private final CacheManager cacheManager;

    public CacheMetrics(MeterRegistry registry, CacheManager cacheManager) {
        this.cacheHits = new ConcurrentHashMap<>();
        this.cacheMisses = new ConcurrentHashMap<>();
        this.registry = registry;
        this.cacheManager = cacheManager;

        initializeMetrics();
    }

    private void initializeMetrics() {
        // Cache size metric
        Gauge.builder("cache.size", cacheManager, this::getCacheSize)
                .tag(Constants.CACHE, AUTH_CACHE)
                .description("Number of entries in cache")
                .register(registry);

        // Cache hits metric
        Gauge.builder("cache.hits", cacheManager, this::getCacheHits)
                .tag(Constants.CACHE, AUTH_CACHE)
                .description("Number of cache hits")
                .register(registry);

        // Cache misses metric
        Gauge.builder("cache.misses", cacheManager, this::getCacheMisses)
                .tag(Constants.CACHE, AUTH_CACHE)
                .description("Number of cache misses")
                .register(registry);

        // Hit ratio metric
        Gauge.builder("cache.hit.ratio", this, this::getHitRatio)
                .tag(Constants.CACHE, AUTH_CACHE)
                .description("Cache hit ratio")
                .register(registry);
    }

    private long getCacheSize(CacheManager cacheManager) {
        Cache cache = cacheManager.getCache(AUTH_CACHE);
        if (cache instanceof ConcurrentMapCache concurrentmapcache) {
            return concurrentmapcache.getNativeCache().size();
        }
//        if (cache instanceof ConcurrentMapCache) {
//            return ((ConcurrentMapCache) cache).getNativeCache().size();
//        }
        return 0;
    }

    // Simple implementation
//    private long getCacheHits(CacheManager cacheManager) {
//        Cache cache = cacheManager.getCache(AUTH_CACHE);
//        if (cache != null) {
//            Object nativeCache = cache.getNativeCache();
//            if (nativeCache instanceof Map) {
//                return ((Map<?, ?>) nativeCache).size();
//            } else if (nativeCache instanceof Collection) {
//                return ((Collection<?>) nativeCache).size();
//            } else {
//                // Handle the case where nativeCache is neither a Map nor a Collection
//                return 0;
//            }
//        } else {
//            return 0;
//        }
//    }

    // Implementation with CacheStatistics
//    private long getCacheHits(CacheManager cacheManager) {
//        Cache cache = cacheManager.getCache(AUTH_CACHE);
//        if (cache != null) {
//            CacheStatistics stats = cache.getStatistics();
//            if (stats != null) {
//                return stats.getHitCount();
//            }
//        }
//        return 0;
//    }

    // Implementations with AtomicLong

    private long getCacheHits(CacheManager cacheManager) {
        return cacheHits.computeIfAbsent(AUTH_CACHE, k -> new AtomicLong(0)).get();
    }

    private long getCacheMisses(CacheManager cacheManager) {
        return cacheMisses.computeIfAbsent(AUTH_CACHE, k -> new AtomicLong(0)).get();
    }

    // Hit ratio
    // Simple implementation
//    private double getHitRatio() {
//        long hits = getCacheHits(cacheManager);
//        long total = hits + getCacheMisses(cacheManager);
//        return total == 0 ? 0.0 : (double) hits / total;
//    }

    private double getHitRatio(CacheMetrics cacheMetrics) {
        long hits = cacheMetrics.getCacheHits(cacheManager);
        long total = hits + cacheMetrics.getCacheMisses(cacheManager);
        return total == 0 ? 0.0 : (double) hits / total;
    }

    // Methods to record hits and misses
    public void recordCacheHit(String cacheName) {
        cacheHits.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
        registry.counter("cache.access", Constants.CACHE, cacheName, "result", "hit").increment();
    }

    public void recordCacheMiss(String cacheName) {
        cacheMisses.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
        registry.counter("cache.access", Constants.CACHE, cacheName, "result", "miss").increment();
    }
    // Other methods to record hits and misses
}
