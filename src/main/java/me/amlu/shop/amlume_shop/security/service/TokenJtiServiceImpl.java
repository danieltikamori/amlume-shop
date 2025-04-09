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

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RScoredSortedSet;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@Slf4j
public class TokenJtiServiceImpl implements TokenJtiService {

    // Define a prefix for JTI keys in Redis
    private static final String JTI_KEY_PREFIX = "jti:";

    private final RedisTemplate<String, String> redisTemplate; // Using <String, String> as value is not important
    private final ExecutorService virtualThreadExecutor;

    public TokenJtiServiceImpl(RedisTemplate<String, String> redisTemplate, // Inject RedisTemplate
                               ExecutorService virtualThreadExecutor) {
        this.redisTemplate = redisTemplate;
        this.virtualThreadExecutor = virtualThreadExecutor;
        log.info("TokenJtiService initialized using RedisTemplate (Valkey).");
    }

    /**
     * Checks if a JTI exists in Redis and has not expired.
     * Redis handles TTL expiration automatically.
     *
     * @param jti The JWT ID to validate.
     * @return true if the JTI exists and is considered valid, false otherwise.
     */
    @Override
    public boolean isJtiValid(String jti) {
        if (jti == null || jti.isBlank()) {
            log.trace("Attempted to validate null or blank JTI.");
            return false;
        }
        String redisKey = JTI_KEY_PREFIX + jti;

        // Use CompletableFuture for potential I/O offloading, but wait for the result.
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // Check if the key exists. Redis automatically handles TTL.
                    boolean isValid = redisTemplate.hasKey(redisKey);
                    if (isValid) {
                        log.trace("JTI '{}' found in Redis (valid).", jti);
                    } else {
                        log.trace("JTI '{}' not found in Redis (invalid or expired).", jti);
                    }
                    return isValid;
                } catch (Exception e) {
                    // Log Redis errors during the check
                    log.error("Error checking JTI '{}' validity in Redis", redisKey, e);
                    // Fail-safe: Treat as invalid on error
                    return false;
                }
            }, virtualThreadExecutor).join(); // Wait for the validation result
        } catch (CompletionException e) {
            // Handle exceptions thrown from within the CompletableFuture lambda itself
            log.error("CompletionException while checking JTI '{}' validity", redisKey, e.getCause());
            return false; // Fail-safe: Treat as invalid on error
        } catch (Exception e) {
            // Handle other potential errors (e.g., issues submitting to executor)
            log.error("Unexpected error while checking JTI '{}' validity", redisKey, e);
            return false; // Fail-safe: Treat as invalid on error
        }
    }

    /**
     * Stores a JTI in Redis with a specific Time-To-Live (TTL).
     * Runs asynchronously (fire-and-forget).
     *
     * @param jti      The JWT ID to store.
     * @param duration The duration for which the JTI should be considered valid (TTL).
     */
    @Override
    public void storeJti(String jti, Duration duration) {
        if (jti == null || jti.isBlank() || duration == null || duration.isNegative() || duration.isZero()) {
            log.warn("Attempted to store JTI with invalid arguments. JTI: {}, Duration: {}", jti, duration);
            return;
        }
        String redisKey = JTI_KEY_PREFIX + jti;

        // Use CompletableFuture for fire-and-forget storage
        CompletableFuture.runAsync(() -> {
            try {
                // Store the key with an empty value and the specified TTL
                // The presence of the key indicates validity.
                redisTemplate.opsForValue().set(redisKey, "", duration);
                log.trace("Stored JTI '{}' in Redis with TTL: {}", redisKey, duration);
            } catch (Exception e) {
                // Log Redis errors during storage
                log.error("Error storing JTI '{}' in Redis", redisKey, e);
                // Depending on requirements, might need retry logic or other handling
            }
        }, virtualThreadExecutor).exceptionally(ex -> {
            // Handle exceptions thrown from the async task itself
            log.error("Exception in async JTI storage task for key '{}'", redisKey, ex);
            return null; // Required for exceptionally
        });
        // No .join() - Let it run asynchronously
    }

    /**
     * Initialization method required by the interface.
     * No explicit initialization is needed for this Redis-based implementation.
     */
    @Override
    public void initialize() {
        // No specific initialization required when using Redis keys with TTL.
        log.info("TokenJtiService.initialize() called - no specific action needed for Redis TTL implementation.");
    }

}
