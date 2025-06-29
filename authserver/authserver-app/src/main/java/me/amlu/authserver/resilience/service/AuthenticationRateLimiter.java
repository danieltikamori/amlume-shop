/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.resilience.service;

import io.lettuce.core.cluster.PartitionException;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;

import me.amlu.authserver.exceptions.RateLimitExceededException;
import me.amlu.authserver.exceptions.RateLimitException;
import me.amlu.authserver.resilience.ratelimiter.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import static me.amlu.authserver.common.CacheKeys.AUTH_SW_RATELIMIT_KEY;

/**
 * Service responsible for managing authentication rate limits using a Redis-backed sliding window algorithm.
 * It provides methods to verify rate limits for both IP addresses and usernames, and handles
 * cases where the rate limit is exceeded or the rate limiting service is unavailable.
 * <p>
 * This service integrates with Micrometer for metrics collection and uses SLF4J for logging.
 * </p>
 */
@Service
public class AuthenticationRateLimiter implements AuthenticationRateLimitInterface {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationRateLimiter.class);

    private final RateLimiter rateLimiter; // Injected RateLimiter implementation (e.g., RedisSlidingWindowRateLimiter)
    private final MeterRegistry meterRegistry;

    public AuthenticationRateLimiter(
            @Qualifier("redisSlidingWindowRateLimiter") RateLimiter rateLimiter,
            MeterRegistry meterRegistry) { // Inject the qualified executor
        this.rateLimiter = rateLimiter;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Verifies the authentication rate limit for a given client IP address.
     * This method uses a sliding window algorithm to determine if the IP has exceeded
     * the configured request threshold within a specific time frame.
     * <p>
     * If the rate limit is exceeded, a warning is logged and a counter is incremented.
     * If the rate limiting check itself fails (e.g., Redis connection issues), a {@link RateLimitException} is thrown,
     * effectively failing the request (fail-closed approach).
     *
     * @param clientIp The IP address of the client.
     * @throws RateLimitExceededException If the rate limit is hit
     */
    @Timed(value = "authserver.auth-ratelimit.ip-ratelimit", description = "Time taken to perform rate limit check for givenIP address")
    public void verifyIpRateLimit(String clientIp)
            throws RateLimitExceededException {

        verifyAuthRatelimit(clientIp);
    }

    /**
     * Verifies the authentication rate limit for a given username.
     * Similar to {@link #verifyIpRateLimit(String)}, this method applies a sliding window
     * rate limit to prevent brute-force attacks or excessive login attempts for a specific user.
     * <p>
     * If the rate limit is exceeded, a warning is logged and a counter is incremented.
     * If the rate limiting check itself fails, a {@link RateLimitException} is thrown.
     *
     * @param username The username of the client.
     * @throws RateLimitExceededException If the rate limit is hit
     */
    @Timed(value = "authserver.auth-ratelimit.username-ratelimit", description = "Time taken to perform rate limit check for given username")
    public void verifyUsernameRateLimit(String username)
            throws RateLimitExceededException {

        verifyAuthRatelimit(username);
    }

    /**
     * Internal helper method to perform the actual rate limit check using the injected {@link RateLimiter}.
     * It constructs the rate limit key, attempts to acquire a permit, and handles various exceptions
     * that might occur during the rate limiting process.
     * <p>
     * This method implements a "fail-closed" strategy: if the rate limiting service is unavailable
     * or encounters an unexpected error, the request is denied by throwing a {@link RateLimitException}.
     * </p>
     *
     * @param identifier The unique identifier for the rate limit (e.g., client IP or username).
     */
    private void verifyAuthRatelimit(String identifier) {
        boolean permitAcquired;
        String rateLimitKey = AUTH_SW_RATELIMIT_KEY + identifier; // Construct the key with limiter name

        try {
            // 1. Check Rate Limit using the injected service (Sliding Window Lua Script)
            permitAcquired = rateLimiter.tryAcquire(rateLimitKey);
            // Note: The RateLimitExceededException is not thrown here directly, but rather handled by the caller
            // based on the 'permitAcquired' boolean. This allows for more flexible handling (e.g., CAPTCHA).
        } catch (RateLimitException e) { // Catch exception if Redis failed and failOpen=false
            // Handle failure during rate limit check (e.g., Redis connection issue)
            log.error("Rate limiting check failed for IP: {}. Denying request. (fail-closed).", identifier, e);
            meterRegistry.counter("auth.ratelimit.check.error").increment();
            // Fail-closed by throwing an exception here.
            // Fail-open: Allow the request if rate limiting check fails.
            throw new RateLimitException("Rate limiter unavailable.", e);
        } catch (Exception e) { // Catch unexpected exceptions during acquire
            log.error("Unexpected error during rate limit check for IP: {}.", identifier, e);
            meterRegistry.counter("auth.ratelimit.check.error").increment();
            throw new RateLimitException("Unexpected error during rate limit check.", e);
        }

        if (permitAcquired) {
            // Rate limit OK
            log.trace("Rate limit check passed for identifier: {}", identifier);
        } else {
            // Rate limit EXCEEDED
            log.warn("Rate limit exceeded for identifier: {}.", identifier);
            meterRegistry.counter("auth.ratelimit.exceeded").increment(); // Corrected typo: aut -> auth
            // Throw RateLimitExceededException to signal to the caller that the limit has been hit.
            // This allows the caller to decide on the next action (e.g., return 429, require CAPTCHA).
            throw new RateLimitExceededException("Rate limit exceeded for " + identifier);
        }
    }


    // --- Helper methods for Resilience4j (Retry predicate, Fallback, etc.) ---

    /**
     * Determines if a given {@link Throwable} is a retryable exception for Redis operations.
     * This is typically used in conjunction with Resilience4j's Retry mechanism.
     * Exceptions like connection failures or data access issues are considered retryable,
     * while {@link RateLimitExceededException} is explicitly not retryable as it signifies
     * a business logic condition rather than a transient infrastructure issue.
     *
     * @param e The throwable to check.
     * @return true if the exception is retryable, false otherwise.
     */
    private boolean isRetryableException(Throwable e) {
        // Define which exceptions should trigger a retry for Redis operations
        // Broader Spring Data exception
        // Lettuce cluster exception
        // Thread interruption
        // Add other specific connection or transient errors if necessary
        // e.g., io.lettuce.core.RedisConnectionException
        // e.g., org.springframework.data.redis.RedisConnectionFailureException (if using Spring Data Redis)
        // DO NOT retry RateLimitExceededException as it's a business logic outcome.
        return
                !(e instanceof RateLimitExceededException);
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
     * Gets the approximate remaining rate limit permits for a client IP.
     * For a sliding window rate limiter, calculating the exact remaining count without
     * an additional Redis query (e.g., ZCARD) can be complex or misleading.
     * This method delegates to the underlying {@link RateLimiter}'s {@code getRemainingPermits} method.
     * If an error occurs during this retrieval, it logs the error and returns -1,
     * indicating that the remaining count could not be determined.
     *
     * @param clientIp The client IP address.
     * @return The approximate number of remaining permits, or -1 if an error occurs.
     */
    public long getRemainingIpLimit(String clientIp) {
        // Calculating the exact remaining limit for a sliding window requires querying the sorted set size again.
        // For simplicity and performance,
        // we can return -1 or a placeholder indicating it's not directly calculated here.
        // Alternatively, you could execute ZCARD again, but that adds overhead.
        String rateLimitKey = AUTH_SW_RATELIMIT_KEY + clientIp;
        try {
            return rateLimiter.getRemainingPermits(rateLimitKey);
        } catch (Exception e) {
            log.error("Error getting remaining captcha limit for IP: {}", clientIp, e);
            log.trace("getRemainingLimit is approximate for sliding window. Returning -1 for IP: {}", clientIp);
            return -1; // Indicate not easily calculated or return an approximation if needed.
        }
    }

    /**
     * Gets the approximate remaining rate limit permits for a username.
     * Similar to {@link #getRemainingIpLimit(String)}, this method retrieves the
     * approximate remaining permits for a given username from the underlying rate limiter.
     * Errors during retrieval result in a logged message and a return value of -1.
     *
     * @param username The username.
     * @return The approximate number of remaining permits, or -1 if an error occurs.
     */
    public long getRemainingUsernameLimit(String username) {
        // Calculating the exact remaining limit for a sliding window requires querying the sorted set size again.
        // For simplicity and performance,
        // we can return -1 or a placeholder indicating it's not directly calculated here.
        // Alternatively, you could execute ZCARD again, but that adds overhead.
        String rateLimitKey = AUTH_SW_RATELIMIT_KEY + username;
        try {
            return rateLimiter.getRemainingPermits(rateLimitKey);
        } catch (Exception e) {
            log.error("Error getting remaining captcha limit for username: {}", username, e);
            log.trace("getRemainingLimit is approximate for sliding window. Returning -1 for username: {}", username);
            return -1; // Indicate not easily calculated or return an approximation if needed.
        }
    }

}
