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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Implements a sliding window rate limiter using Valkey/Redis and an atomic Lua script.
 * Uses the "Check Before Add" strategy.
 */
@Service
public class SlidingWindowValkeyRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowValkeyRateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> script; // Script returns Long (1 for success, 0 for fail)
    private final int windowSizeInSeconds;
    private final int maxRequestsPerWindow;

    public SlidingWindowValkeyRateLimiter(
            StringRedisTemplate redisTemplate, // Inject Spring-managed Redis Template
            // Use specific properties for ASN rate limiting, matching the usage context
            @Value("${asn.ratelimit.window-seconds:60}") int windowSizeInSeconds,
            @Value("${asn.ratelimit.max-requests:100}") int maxRequestsPerWindow) {

        this.redisTemplate = redisTemplate;
        this.windowSizeInSeconds = windowSizeInSeconds;
        this.maxRequestsPerWindow = maxRequestsPerWindow;

        // Load the Lua script
        this.script = new DefaultRedisScript<>();
        // Ensure the path matches where you saved the script
        this.script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/sliding_window_rate_limit_check_first.lua")));
        this.script.setResultType(Long.class); // Lua script returns 1 or 0
    }

    /**
     * Attempts to acquire permission for one request for the given identifier.
     *
     * @param identifier A unique identifier for the entity being rate-limited (e.g., IP address).
     * @return true if the request is allowed within the rate limit, false otherwise.
     */
    public boolean tryAcquire(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            log.warn("Attempted rate limiting with null or blank identifier.");
            return false; // Or handle as appropriate
        }

        // Construct a specific key for ASN rate limiting
        String key = "rate_limit:asn:" + identifier;
        long nowMillis = System.currentTimeMillis();
        long windowMillis = this.windowSizeInSeconds * 1000L;

        try {
            // Execute the Lua script atomically
            Long result = redisTemplate.execute(
                    script,
                    Collections.singletonList(key),    // KEYS[1]
                    // Arguments must match the Lua script's ARGV order
                    String.valueOf(windowMillis),      // ARGV[1] (windowMillis)
                    String.valueOf(maxRequestsPerWindow), // ARGV[2] (maxRequests)
                    String.valueOf(nowMillis)          // ARGV[3] (now)
            );

            // Lua script returns 1 if allowed, 0 if denied
            boolean allowed = result == 1L;

            if (!allowed) {
                log.trace("Rate limit denied for identifier: {}", identifier);
            } else {
                log.trace("Rate limit allowed for identifier: {}", identifier);
            }
            return allowed;

        } catch (DataAccessException e) {
            // Handle potential Redis errors (e.g., connection issues)
            log.error("Redis error during rate limiting check for identifier [{}]: {}", identifier, e.getMessage());
            // Fail-open strategy: Allow request if Redis fails. Change to 'false' for fail-closed.
            return true;
        } catch (Exception e) {
            // Catch unexpected errors during script execution
            log.error("Unexpected error during rate limiting check for identifier [{}]: {}", identifier, e.getMessage(), e);
            return true; // Fail-open
        }
    }
}
