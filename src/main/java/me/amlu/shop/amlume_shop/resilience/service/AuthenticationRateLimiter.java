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

import io.lettuce.core.cluster.PartitionException;
import io.micrometer.core.instrument.MeterRegistry;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.RateLimitExceededException;
import me.amlu.shop.amlume_shop.exceptions.RateLimitException;
import me.amlu.shop.amlume_shop.resilience.ratelimiter.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationRateLimiter implements AuthenticationRateLimitInterface {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationRateLimiter.class);

    private final RateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;

    public AuthenticationRateLimiter(
            @Qualifier("redisSlidingWindowRateLimiter") RateLimiter rateLimiter,
            MeterRegistry meterRegistry) { // Inject the qualified executor
        this.rateLimiter = rateLimiter;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Validates the rate limit for a client IP using a sliding window.
     * If the limit is exceeded, it then fails the request.
     *
     * @param clientIp The IP address of the client.
     * @throws RateLimitExceededException If the rate limit is hit
     */
    public void verifyIpRateLimit(String clientIp)
            throws RateLimitExceededException {

        verifyAuthRatelimit(clientIp);
    }

    /**
     * Validates the rate limit for a client IP using a sliding window.
     * If the limit is exceeded, it then validates the provided CAPTCHA response.
     *
     * @param username The username of the client.
     * @throws RateLimitExceededException If the rate limit is hit
     */
    public void verifyUsernameRateLimit(String username)
            throws RateLimitExceededException {

        verifyAuthRatelimit(username);
    }

    private void verifyAuthRatelimit(String identifier) {
        boolean permitAcquired;
        String rateLimitKey = Constants.AUTH_SW_RATELIMIT_KEY + identifier; // Construct the key with limiter name

        try {
            // 1. Check Rate Limit using the injected service (Sliding Window Lua Script)
            permitAcquired = rateLimiter.tryAcquire(rateLimitKey);

        } catch (RateLimitException e) { // Catch exception if Redis failed and failOpen=false
            // Handle failure during rate limit check (e.g., Redis connection issue)
            log.error("Rate limiting check failed for IP: {}. Denying request. (fail-closed).", identifier, e);
            meterRegistry.counter("auth.ratelimit.check.error").increment();
            // Fail-closed by throwing an exception here.
            // Fail-open: Allow the request if rate limiting check fails.
            throw new RateLimitException("Rate limiter unavailable.", e);
//            return; // Allow request
        } catch (Exception e) { // Catch unexpected exceptions during acquire
            log.error("Unexpected error during rate limit check for IP: {}.", identifier, e);
            meterRegistry.counter("auth.ratelimit.check.error").increment();
            throw new RateLimitException("Unexpected error during rate limit check.", e);
        }

        if (permitAcquired) {
            // Rate limit OK
            log.trace("Rate limit check passed for IP: {}", identifier);
//            return; // Success
        } else {
            // Rate limit EXCEEDED
            log.warn("Rate limit exceeded for IP: {}. CAPTCHA validation required.", identifier);
            meterRegistry.counter("aut.ratelimit.exceeded").increment();
        }
    }


    // --- Helper methods for Resilience4j (Retry predicate, Fallback, etc.) ---

    // No changes needed for these helper methods based on the TimeLimiter integration
    private boolean isRetryableException(Throwable e) {
        // Define which exceptions should trigger a retry for Redis operations
        return
//                e instanceof RedisConnectionFailureException || // Spring Data Redis connection exception
                e instanceof DataAccessException || // Broader Spring Data exception
                        e instanceof PartitionException || // Lettuce cluster exception
                        e instanceof InterruptedException;
        // DO NOT retry RateLimitExceededException
    }

    // Fallback method example (if needed for circuit breakers)
    private boolean rateLimitFallback(String clientIp, Throwable t) {
        log.error("Rate limiting fallback triggered for IP: {} due to: {}", clientIp, t.getMessage());
        meterRegistry.counter("auth.ratelimit.fallback").increment();
        // Decide fail-open or fail-closed
        return true; // Fail-open example
    }

    // --- Utility Methods ---

    /**
     * Gets the approximate remaining rate limit permits for a client IP using Redis Sliding Window.
     * Note: This is an estimate. Calculating the exact remaining count requires another ZCARD call.
     * Returning -1 to indicate that an exact count isn't readily available without another query.
     *
     * @param clientIp The client IP address.
     * @return -1, indicating an exact count is not easily determined without another query.
     */
    public long getRemainingIpLimit(String clientIp) {
        // Calculating the exact remaining limit for a sliding window requires querying the sorted set size again.
        // For simplicity and performance,
        // we can return -1 or a placeholder indicating it's not directly calculated here.
        // Alternatively, you could execute ZCARD again, but that adds overhead.
        String rateLimitKey = Constants.AUTH_SW_RATELIMIT_KEY + clientIp;
        try {
            return rateLimiter.getRemainingPermits(rateLimitKey);
        } catch (Exception e) {
            log.error("Error getting remaining captcha limit for IP: {}", clientIp, e);
            log.trace("getRemainingLimit is approximate for sliding window. Returning -1 for IP: {}", clientIp);
            return -1; // Indicate not easily calculated or return an approximation if needed.
        }
    }

    /**
     * Gets the approximate remaining rate limit permits for a username using Redis Sliding Window.
     *
     * @param username The username.
     *                 Note: This is an estimate. Calculating the exact remaining count requires another ZCARD call.
     * @return Returning -1 to indicate that an exact count isn't readily available without another query.
     */
    public long getRemainingUsernameLimit(String username) {
        // Calculating the exact remaining limit for a sliding window requires querying the sorted set size again.
        // For simplicity and performance,
        // we can return -1 or a placeholder indicating it's not directly calculated here.
        // Alternatively, you could execute ZCARD again, but that adds overhead.
        String rateLimitKey = Constants.AUTH_SW_RATELIMIT_KEY + username;
        try {
            return rateLimiter.getRemainingPermits(rateLimitKey);
        } catch (Exception e) {
            log.error("Error getting remaining captcha limit for username: {}", username, e);
            log.trace("getRemainingLimit is approximate for sliding window. Returning -1 for username: {}", username);
            return -1; // Indicate not easily calculated or return an approximation if needed.
        }
    }

}
