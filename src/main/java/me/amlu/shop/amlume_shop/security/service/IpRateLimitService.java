/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class IpRateLimitService {
    private final LoadingCache<String, RateLimiter> ipRateLimiters;
    private final LoadingCache<String, AtomicInteger> burstCounters;

    public IpRateLimitService() {
        this.ipRateLimiters = CacheBuilder.newBuilder()
                .maximumSize(100_000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(new CacheLoader<>() {
                    @NotNull
                    @Override
                    public RateLimiter load(@NotNull String ip) {
                        return RateLimiter.create(10.0); // 10 requests per second per IP
                    }
                });

        this.burstCounters = CacheBuilder.newBuilder()
                .maximumSize(100_000)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @NotNull
                    @Override
                    public AtomicInteger load(@NotNull String ip) {
                        return new AtomicInteger(0);
                    }
                });
    }

    @Async
    public CompletableFuture<Boolean> checkRateLimit(String ip) {
        try {
            RateLimiter limiter = ipRateLimiters.get(ip);
            AtomicInteger counter = burstCounters.get(ip);

            // Check burst limit
            if (counter.incrementAndGet() > 100) { // 100 requests per minute
                log.warn("Burst limit exceeded for IP: {}", ip);
                return CompletableFuture.completedFuture(false);
            }

            // Apply token bucket rate limiting
            boolean allowed = limiter.tryAcquire(100, TimeUnit.MILLISECONDS);
            if (!allowed) {
                log.warn("Rate limit exceeded for IP: {}", ip);
            }
            return CompletableFuture.completedFuture(allowed);

        } catch (ExecutionException e) {
            log.error("Error checking rate limit for IP: {}", ip, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Scheduled(fixedRate = 60000) // Every minute
    public void resetBurstCounters() {
        burstCounters.cleanUp();
    }
}
