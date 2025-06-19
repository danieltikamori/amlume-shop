/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

/**
 * Simple in-memory rate limiter to prevent brute force attacks.
 * For production use, consider using a distributed rate limiter with Redis.
 */
@Component
public class RateLimiter {
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    // Default limits
    private static final int DEFAULT_MAX_REQUESTS = 5;
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(1);

    /**
     * Checks if a request from the given key should be rate limited.
     *
     * @param key The key to identify the requester (e.g., IP address, username)
     * @return true if the request is allowed, false if it should be limited
     */
    public boolean allowRequest(String key) {
        return allowRequest(key, DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW);
    }

    /**
     * Checks if a request from the given key should be rate limited.
     *
     * @param key         The key to identify the requester
     * @param maxRequests Maximum number of requests allowed in the time window
     * @param window      Time window for rate limiting
     * @return true if the request is allowed, false if it should be limited
     */
    public boolean allowRequest(String key, int maxRequests, Duration window) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        long now = System.currentTimeMillis();
        RequestCounter counter = requestCounts.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart > window.toMillis()) {
                // First request or window expired, create new counter
                return new RequestCounter(now, 1);
            } else {
                // Increment existing counter
                existing.count.incrementAndGet();
                return existing;
            }
        });

        // Clean up old entries periodically
        if (requestCounts.size() > 10000) {
            cleanupOldEntries(window);
        }

        return counter.count.get() <= maxRequests;
    }

    private void cleanupOldEntries(Duration window) {
        long cutoff = System.currentTimeMillis() - window.toMillis();
        requestCounts.entrySet().removeIf(entry -> entry.getValue().windowStart < cutoff);
    }

    private static class RequestCounter {
        final long windowStart;
        final AtomicInteger count;

        RequestCounter(long windowStart, int initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(initialCount);
        }
    }
}
