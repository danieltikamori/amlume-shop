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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.*;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.config.properties.AsnProperties;
import me.amlu.shop.amlume_shop.model.Role;
import me.amlu.shop.amlume_shop.security.model.TokenData;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Configures the cache manager for the application.
 */
@Slf4j
@EnableCaching
@Configuration
public class CacheConfig {

    private static final int CACHE_INITIAL_CAPACITY = Math.min(100, Runtime.getRuntime().availableProcessors() * 10);
    public static final int ESTIMATED_ENTRY_SIZE = 1024;
    private static final double MAX_HEAP_RATIO = 0.1; // Use max 10% of heap
    private static final long CACHE_MAXIMUM_SIZE =
            (long) (Runtime.getRuntime().maxMemory() * MAX_HEAP_RATIO / ESTIMATED_ENTRY_SIZE);
    private static final int CACHE_EXPIRATION_MINUTES = 60;
    private static final int CACHE_MAXIMUM_MEMORY_MB = 100;
    public static final int CACHE_REFRESH_MINUTES = 30; // CACHE_REFRESH_MINUTES

    private static final int CACHE_CONCURRENCY_LEVEL = Runtime.getRuntime().availableProcessors();
    private static final long CACHE_EXPIRATION_AFTER_WRITE = 60 * 60 * 1000; // 1 hour
    private static final long CACHE_EXPIRATION_AFTER_ACCESS = 30 * 60 * 1000;
    private static final long CACHE_REFRESH_AFTER_WRITE = 30 * 60 * 1000;
    private static final long CACHE_REFRESH_AFTER_ACCESS = 15 * 60 * 1000;
    private static final int CACHE_MAXIMUM_WEIGHT = 100;
    private static final long CACHE_WEIGHTED_EXPIRATION_AFTER_WRITE = 60 * 60 * 1000;
    private static final long CACHE_WEIGHTED_EXPIRATION_AFTER_ACCESS = 30 * 60 * 1000;
    private static final long CACHE_WEIGHTED_REFRESH_AFTER_WRITE = 30 * 60 * 1000;
    private static final long CACHE_WEIGHTED_REFRESH_AFTER_ACCESS = 15 * 60 * 1000;
    private static final int CACHE_WEIGHTED_MAXIMUM_WEIGHT = 100;
    private static final long CACHE_WEIGHTED_EXPIRATION_AFTER_WRITE_JITTER = 5 * 60 * 1000;
    private static final long CACHE_WEIGHTED_EXPIRATION_AFTER_ACCESS_JITTER = 2 * 60 * 1000;
    private static final long CACHE_WEIGHTED_REFRESH_AFTER_WRITE_JITTER = 2 * 60 * 1000;
    private static final long CACHE_WEIGHTED_REFRESH_AFTER_ACCESS_JITTER = 1 * 60 * 1000;
    private static final int CACHE_WEIGHTED_MAXIMUM_WEIGHT_JITTER = 10;
    private static final long CACHE_WEIGHTED_EXPIRATION_AFTER_WRITE_JITTER_RATIO = 10;
    private static final long CACHE_WEIGHTED_EXPIRATION_AFTER_ACCESS_JITTER_RATIO = 5;
    private static final long CACHE_WEIGHTED_REFRESH_AFTER_WRITE_JITTER_RATIO = 5;
    private static final long CACHE_WEIGHTED_REFRESH_AFTER_ACCESS_JITTER_RATIO = 2;
    private static final int CACHE_WEIGHTED_MAXIMUM_WEIGHT_JITTER_RATIO = 20;

    private final MetricRegistry metricRegistry;

    public CacheConfig(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

//    /**
//     * Create a cache manager to support caching of the current user and the current user's ID.
//     * <p>
//     * The cache manager is a simple cache manager using a {@link ConcurrentMapCache} as the cache implementation.
//     * <p>
//     * The cache manager supports two caches: "currentUser" and "currentUserId".
//     * <p>
//     * The cache manager is a Spring bean, and can be injected into other components.
//     * <p>
//     *
//     * @return a cache manager
//     */
//
//    @Bean
//    public CacheManager cacheManager() {
//        SimpleCacheManager cacheManager = new SimpleCacheManager();
//        cacheManager.setCaches(Arrays.asList(
//                new ConcurrentMapCache("currentUser"),
//                new ConcurrentMapCache("currentUserId"),
//                new ConcurrentMapCache("roles"),
//                new ConcurrentMapCache("rateLimit"),
//                new ConcurrentMapCache("mfaTokens")
//        ));
//
//        return cacheManager;
//    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("mfaTokens", "currentUser", "currentUserId", "roles", "rateLimit", "asn");
    }

    @Bean
    public Cache<String, Set<Role>> rolesCache() {
        return CacheBuilder.newBuilder()
                .concurrencyLevel(Runtime.getRuntime().availableProcessors())
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(10000)
                .recordStats() // enables statistics
                .build();
    }

    @Bean
    public Cache<String, AtomicInteger> rateLimitCache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .concurrencyLevel(Runtime.getRuntime().availableProcessors())
                .maximumSize(10000)
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, String> asnCache(AsnProperties properties) {
        return CacheBuilder.newBuilder()
                .concurrencyLevel(Runtime.getRuntime().availableProcessors())
                .expireAfterWrite(properties.getCacheExpiration())
                .maximumSize(properties.getCacheSize())
                .recordStats()
                .removalListener(notification ->
                        log.debug("ASN removed from cache. Key: {}, Cause: {}",
                                notification.getKey(), notification.getCause()))
                .build();
    }

    @Bean
    public Cache<String, TokenData> tokenCache() {
        return CacheBuilder.newBuilder()
                .initialCapacity(CACHE_INITIAL_CAPACITY)
                .maximumSize(CACHE_MAXIMUM_SIZE)
                .maximumWeight(CACHE_MAXIMUM_MEMORY_MB * 1024 * 1024)
                .weigher((key, value) -> (int) Optional.of(value)
                        .map(TokenData::getApproximateSize)
                        .orElse(0))  // If token have varying sizes, use Weight-based sizing
                .expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                .concurrencyLevel(Runtime.getRuntime().availableProcessors())
//                .softValues() // Allow Garbage Collector(GC) to collect when memory is low
                .recordStats() // Enable statistics
                .removalListener(this::handleTokenRemoval)
                .build();
    }

    private void handleTokenRemoval(RemovalNotification<String, TokenData> notification) {
        log.debug("Token removed from cache. Key: {}, Cause: {}",
                notification.getKey(), notification.getCause());

        if (notification.getCause().equals(RemovalCause.SIZE)) {
            log.warn("Token evicted due to size limits. Consider adjusting cache size.");
        }
    }

    @Bean
    public CacheMonitor cacheMonitor(Cache<String, TokenData> tokenCache) {
        return new CacheMonitor(tokenCache, metricRegistry);
    }

    @Component
    @Slf4j
    public class CacheMonitor {
        private final Cache<String, TokenData> tokenCache;
        private final MetricRegistry metricRegistry;
        private static final String METRIC_PREFIX = "cache.";
        private final Map<String, Gauge<?>> gauges = new ConcurrentHashMap<>();

        public CacheMonitor(Cache<String, TokenData> tokenCache, MetricRegistry metricRegistry) {
            this.tokenCache = tokenCache;
            this.metricRegistry = metricRegistry;
            initializeGauges();
        }

        private void initializeGauges() {
            registerGauge("hit.rate", CacheStats::hitRate);
            registerGauge("miss.rate", CacheStats::missRate);
            registerGauge("load.exception.rate", CacheStats::loadExceptionRate);
            registerGauge("eviction.count", CacheStats::evictionCount);
            registerGauge("average.load.penalty", CacheStats::averageLoadPenalty);
        }

        private void registerGauge(String name, Function<CacheStats, Number> valueFunction) {
            String metricName = METRIC_PREFIX + name;
            gauges.put(metricName, metricRegistry.gauge(metricName,
                    () -> () -> valueFunction.apply(tokenCache.stats())));
        }

        @Scheduled(fixedRateString = "${cache.stats.report.interval:60000}")
        public void reportStats() {
            try {
                CacheStats stats = tokenCache.stats();
                logCacheStats(stats);
            } catch (Exception e) {
                log.error("Error reporting cache stats: {}", e.getMessage(), e);
            }
        }

        private void logCacheStats(CacheStats stats) {
            if (log.isDebugEnabled()) {
                log.debug("""
                                Cache Stats:
                                Hit Rate: {}
                                Miss Rate: {}
                                Load Exception Rate: {}
                                Eviction Count: {}
                                Average Load Penalty: {}""",
                        stats.hitRate(),
                        stats.missRate(),
                        stats.loadExceptionRate(),
                        stats.evictionCount(),
                        stats.averageLoadPenalty());
            }
        }
    }

/**
 * Gauge Registration Optimization :
 * Moved gauge registration to initialization time instead of updating on every metrics report
 * Used a ConcurrentHashMap to store gauges for thread safety
 * Implemented a more efficient gauge registration system
 *
 * Constants and String Management :
 * Added METRIC_PREFIX constant to avoid string concatenation
 * Used text block for more readable log message formatting
 *
 * Performance Improvements :
 * Removed redundant updateMetrics method since gauges now automatically update
 * Reduced the number of method calls to tokenCache.stats()
 * Implemented function-based metric registration
 *
 * Error Handling :
 * Improved error logging by including the error message in the log
 * Used a more structured approach to exception handling
 *
 * Code Organization :
 * Better separation of concerns between initialization and monitoring
 * More maintainable and cleaner code structure
 *
 * Memory Efficiency :
 * Reduced object creation during metric updates
 * Better memory management through proper gauge registration
 *
 * The optimized code provides better performance by:
 * Reducing the number of object creations
 * Minimizing method calls
 * Improving thread safety
 * Better memory management
 * More efficient metric updates
 */

}
