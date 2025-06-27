/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.resilience;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.MaxRetriesExceeded;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.function.Supplier;

/**
 * <p>A utility component for handling rate-limited operations with an exponential backoff strategy.</p>
 *
 * <p>This class provides methods to execute an operation that might be subject to rate limiting.
 * If a {@link RequestNotPermitted} exception is thrown, it retries the operation after an
 * exponentially increasing delay, up to a maximum number of retries and a maximum backoff time.
 * Jitter is added to the backoff time to prevent thundering herd problems.</p>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * @Service
 * public class MyService {
 *
 *     private final ExponentialBackoffRateLimiter rateLimiter;
 *     private final MyApiClient apiClient;
 *
 *     public MyService(ExponentialBackoffRateLimiter rateLimiter, MyApiClient apiClient) {
 *         this.rateLimiter = rateLimiter;
 *         this.apiClient = apiClient;
 *     }
 *
 *     public String fetchDataFromRateLimitedApi() throws InterruptedException {
 *         return rateLimiter.executeWithBackoff(() -> {
 *             // This is the operation that might throw RequestNotPermitted
 *             return apiClient.callExternalService();
 *         });
 *     }
 *
 *     public void sendDataToRateLimitedApi() throws InterruptedException {
 *         rateLimiter.executeWithBackoff(() -> {
 *             apiClient.sendData();
 *         });
 *     }
 * }
 * }</pre>
 *
 * <p>Important Notes:</p>
 * <ul>
 *     <li>This class catches {@link RequestNotPermitted} exceptions specifically from Resilience4j.
 *         Other exceptions will propagate immediately.</li>
 *     <li>The {@code executeWithBackoff} methods can throw {@link InterruptedException} if the
 *         thread is interrupted during the backoff sleep. Callers should handle this.</li>
 *     <li>If the maximum number of retries is exceeded, a {@link MaxRetriesExceeded} exception
 *         is thrown, wrapping the last {@link RequestNotPermitted} exception.</li>
 *     <li>The backoff parameters (initial delay, max delay, max retries, multiplier, jitter)
 *         are configurable via constants.</li>
 * </ul>
 */
@Component
public class ExponentialBackoffRateLimiter {
    /**
     * The initial backoff delay in milliseconds (1 second).
     */
    private static final long INITIAL_BACKOFF_MS = 1000L;
    /**
     * The maximum backoff delay in milliseconds (1 minute).
     */
    private static final long MAX_BACKOFF_MS = 60000L;
    /**
     * The maximum number of retries before giving up.
     */
    private static final int MAX_RETRIES = 5; // Total attempts = 1 (initial) + 5 (retries) = 6
    /**
     * The multiplier for the exponential backoff (e.g., 2.0 for doubling).
     */
    private static final double MULTIPLIER = 2.0;
    /**
     * The factor for adding random jitter to the backoff time (e.g., 0.1 for 10% jitter).
     */
    private static final double JITTER_FACTOR = 0.1;

    private final Random random = new Random();

    /**
     * Executes a supplier operation with exponential backoff and retries in case of rate limiting.
     *
     * @param operation The {@link Supplier} representing the operation to execute.
     *                  This operation is expected to throw {@link RequestNotPermitted} if rate-limited.
     * @param <T>       The type of the result returned by the operation.
     * @return The result of the operation.
     * @throws InterruptedException If the thread is interrupted while sleeping during backoff.
     * @throws MaxRetriesExceeded   If the operation fails due to rate limiting after the maximum number of retries.
     */
    public <T> T executeWithBackoff(final Supplier<T> operation) throws InterruptedException {
        return executeWithRetries(operation, 0);
    }

    /**
     * Recursive helper method to execute the operation with retries.
     *
     * @param operation  The supplier operation.
     * @param retryCount The current retry attempt count (0 for the first attempt).
     * @param <T>        The type of the result.
     * @return The result of the operation.
     * @throws InterruptedException If the thread is interrupted.
     * @throws MaxRetriesExceeded   If max retries are exceeded.
     */
    private <T> T executeWithRetries(final Supplier<T> operation, final int retryCount) throws InterruptedException {
        try {
            return operation.get();
        } catch (RequestNotPermitted e) {
            if (retryCount >= MAX_RETRIES) {
                throw new MaxRetriesExceeded("Max retries (" + MAX_RETRIES + ") exceeded for operation. Last error: " + e.getMessage());
            }

            final long currentBackoff = calculateBackoffTime(retryCount);
            Thread.sleep(currentBackoff);

            return executeWithRetries(operation, retryCount + 1);
        }
    }

    /**
     * Calculates the backoff time for a given retry count, incorporating exponential growth and jitter.
     * The calculated time is capped at {@link #MAX_BACKOFF_MS}.
     *
     * @param retryCount The current retry attempt count.
     * @return The calculated backoff time in milliseconds.
     */
    private long calculateBackoffTime(final int retryCount) {
        final long baseBackoff = (long) (INITIAL_BACKOFF_MS * Math.pow(MULTIPLIER, retryCount));
        // Use nextDouble() for jitter to ensure it's a fraction between 0.0 and 1.0
        // Jitter should be applied as a random percentage of the baseBackoff,
        // and can be both positive and negative to spread out requests more effectively.
        // A common approach is to add/subtract up to JITTER_FACTOR * baseBackoff.
        final long jitter = (long) (baseBackoff * JITTER_FACTOR * (random.nextDouble() * 2 - 1)); // Jitter between -JITTER_FACTOR and +JITTER_FACTOR
        final long backoffWithJitter = baseBackoff + jitter;
        // Ensure backoff time is not negative and capped at MAX_BACKOFF_MS
        return Math.max(0, Math.min(backoffWithJitter, MAX_BACKOFF_MS));
    }

    /**
     * Executes a runnable operation with exponential backoff and retries in case of rate limiting.
     * This is a convenience method for operations that do not return a result.
     *
     * @param operation The {@link Runnable} representing the operation to execute.
     *                  This operation is expected to throw {@link RequestNotPermitted} if rate-limited.
     * @throws InterruptedException If the thread is interrupted while sleeping during backoff.
     * @throws MaxRetriesExceeded   If the operation fails due to rate limiting after the maximum number of retries.
     */
    public void executeWithBackoff(final Runnable operation) throws InterruptedException {
        executeWithBackoff(() -> {
            operation.run();
            return null;
        });
    }
}
