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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.amlu.shop.amlume_shop.config.properties.AsnProperties;
import me.amlu.shop.amlume_shop.model.Role;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configures the cache manager for the application.
 */
@EnableCaching
@Configuration
public class CacheConfig {

    /**
     * Create a cache manager to support caching of the current user and the current user's ID.
     * <p>
     * The cache manager is a simple cache manager using a {@link ConcurrentMapCache} as the cache implementation.
     * <p>
     * The cache manager supports two caches: "currentUser" and "currentUserId".
     * <p>
     * The cache manager is a Spring bean, and can be injected into other components.
     * <p>
     *
     * @return a cache manager
     */

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new ConcurrentMapCache("currentUser"),
                new ConcurrentMapCache("currentUserId"),
                new ConcurrentMapCache("roles"),
                new ConcurrentMapCache("rateLimit")
        ));

        return cacheManager;
    }

    @Bean
    public Cache<String, Set<Role>> rolesCache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(10000)
                .recordStats() // enables statistics
                .build();
    }

    @Bean
    public Cache<String, AtomicInteger> rateLimitCache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10000)
                .recordStats()
                .build();
    }

    @Bean
    public Cache<String, String> asnCache(AsnProperties properties) {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(properties.getCacheExpiration())
                .maximumSize(properties.getCacheSize())
                .recordStats()
                .build();
    }
}
