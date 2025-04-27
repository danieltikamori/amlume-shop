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

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.lettuce.core.cluster.PartitionException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.payload.GetRecaptchaResponse;
import me.amlu.shop.amlume_shop.ratelimiter.RateLimiter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    // Resilience4j instance names (ensure these match your configuration)
    private static final String RECAPTCHA_CIRCUIT_BREAKER = "recaptchaCircuitBreaker";
    private static final String RECAPTCHA_RETRY = "recaptchaRetry";
    private static final String RECAPTCHA_TIME_LIMITER = "recaptchaTimeLimiter";

//    @Value("${RECAPTCHA_SECRET_KEY}")
    @Value("${recaptcha.secret}")
    private String recaptchaSecret;

    private final RestTemplate restTemplate;
    private final RateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;
    private final CircuitBreaker recaptchaCircuitBreaker;
    private final Retry recaptchaRetry;
    private final TimeLimiter recaptchaTimeLimiter;
    private final Clock clock; // For handling clock sync issues
    private final ScheduledExecutorService scheduledExecutorService; // Inject the executor for TimeLimiter

    public CaptchaService(
            RestTemplate restTemplate,
            @Qualifier("redisSlidingWindowRateLimiter") RateLimiter rateLimiter,
            MeterRegistry meterRegistry,
            // Qualify registries if multiple beans exist
            @Qualifier("circuitBreakerRegistry") CircuitBreakerRegistry circuitBreakerRegistry,
            @Qualifier("retryRegistry") RetryRegistry retryRegistry,
            @Qualifier("timeLimiterRegistry") TimeLimiterRegistry timeLimiterRegistry,
            Clock clock,
            @Qualifier("captchaTimeLimiterExecutor") ScheduledExecutorService scheduledExecutorService) { // Inject the qualified executor
        this.restTemplate = restTemplate;
        this.rateLimiter = rateLimiter;
        this.meterRegistry = meterRegistry;
        // Get or create specific instances from registries
        this.recaptchaCircuitBreaker = circuitBreakerRegistry.circuitBreaker(RECAPTCHA_CIRCUIT_BREAKER);
        this.recaptchaRetry = retryRegistry.retry(RECAPTCHA_RETRY);
        this.recaptchaTimeLimiter = timeLimiterRegistry.timeLimiter(RECAPTCHA_TIME_LIMITER);
        this.clock = clock;
        this.scheduledExecutorService = scheduledExecutorService; // Assign the injected executor
    }

    /**
     * Validates the rate limit for a client IP using a sliding window.
     * If the limit is exceeded, it then validates the provided CAPTCHA response.
     *
     * @param clientIp        The IP address of the client.
     * @param captchaResponse The CAPTCHA response string from the client (can be null initially).
     * @throws RateLimitExceededException If the rate limit is hit AND CAPTCHA is required but not provided.
     * @throws InvalidCaptchaException    If the rate limit is hit AND the provided CAPTCHA is invalid.
     * @throws CaptchaServiceException    If an unexpected error occurs during validation.
     */
    public void verifyRateLimitAndCaptcha(String clientIp, String captchaResponse)
            throws RateLimitExceededException, InvalidCaptchaException, CaptchaServiceException {

        boolean permitAcquired;
        String rateLimitKey = Constants.CAPTCHA_RATELIMIT_KEY + clientIp; // Construct the key with limiter name

        try {
            // 1. Check Rate Limit using the injected service (Sliding Window Lua Script)
            permitAcquired = rateLimiter.tryAcquire(rateLimitKey);

        } catch (RateLimitException e) { // Catch exception if Redis failed and failOpen=false
            // Handle failure during rate limit check (e.g., Redis connection issue)
            log.error("Rate limiting check failed for IP: {}. Denying request. (fail-closed).", clientIp, e);
            meterRegistry.counter("captcha.ratelimit.check.error").increment();
            // Fail-closed by throwing an exception here.
            // Fail-open: Allow the request if rate limiting check fails.
            throw new CaptchaServiceException("Rate limiter unavailable.", e);
//            return; // Allow request
        } catch (Exception e) { // Catch unexpected exceptions during acquire
            log.error("Unexpected error during rate limit check for IP: {}.", clientIp, e);
            meterRegistry.counter("captcha.ratelimit.check.error").increment();
            throw new CaptchaServiceException("Unexpected error during rate limit check.", e);
        }

        if (permitAcquired) {
            // Rate limit OK, no CAPTCHA needed for this request
            log.trace("Rate limit check passed for IP: {}", clientIp);
//            return; // Success
        } else {
            // Rate limit EXCEEDED
            log.warn("Rate limit exceeded for IP: {}. CAPTCHA validation required.", clientIp);
            meterRegistry.counter("captcha.ratelimit.exceeded").increment();

            // 2. Check if CAPTCHA was provided
            if (!StringUtils.hasText(captchaResponse)) {
                meterRegistry.counter("captcha.required.missing").increment();
                // Rate limit hit, and no CAPTCHA provided - throw specific exception
                throw new CaptchaRequiredException("CAPTCHA required due to rate limit.");
            }

            // 3. Validate the provided CAPTCHA
            boolean captchaIsValid;
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                // Call the internal validation method which now includes TimeLimiter
                captchaIsValid = validateCaptchaInternal(captchaResponse);
            } catch (Exception e) {
                // Handle failure during CAPTCHA validation (e.g., Google API error, TimeoutException)
                log.error("CAPTCHA validation failed for IP: {} after rate limit hit.", clientIp, e);
                meterRegistry.counter("captcha.validation.error", "context", "after_rate_limit").increment();
                // Treat validation error (including timeout) as invalid CAPTCHA for security
                throw new InvalidCaptchaException("CAPTCHA validation failed.", e);
            } finally {
                sample.stop(meterRegistry.timer("captcha.validation.duration", "context", "after_rate_limit"));
            }

            if (captchaIsValid) {
                // Rate limit hit, but CAPTCHA was valid - allow request
                log.info("Rate limit bypassed for IP: {} with valid CAPTCHA.", clientIp);
                meterRegistry.counter("captcha.passed_after_limit").increment();
                return; // Success
            } else {
                // Rate limit hit, and CAPTCHA was invalid
                log.warn("Invalid CAPTCHA provided for IP: {} after rate limit hit.", clientIp);
                meterRegistry.counter("captcha.invalid_after_limit").increment();
                throw new InvalidCaptchaException("Invalid CAPTCHA provided.");
            }
        }
    }

    /**
     * Performs the actual CAPTCHA validation by calling the Google reCAPTCHA API.
     * Applies Resilience4j Circuit Breaker, Retry, and Time Limiter to the asynchronous call.
     *
     * @param captchaResponse The CAPTCHA response string.
     * @return true if the CAPTCHA is valid, false otherwise.
     * @throws Exception If the validation fails after retries or due to circuit breaker/timeout.
     */
    private boolean validateCaptchaInternal(String captchaResponse) throws Exception {
        // Supplier for the core synchronous API call logic
        Supplier<CompletionStage<GetRecaptchaResponse>> asyncOperationSupplier = getCompletionStageSupplier(captchaResponse);

        // Decorate the *initiation and execution* of the time-limited async operation with Retry.
        // Retry will call this lambda on each attempt.
        Supplier<GetRecaptchaResponse> retryingSupplier = Retry.decorateSupplier(recaptchaRetry, () -> {
            try {
                // Inside the retry attempt, execute the async operation with the TimeLimiter applied.
                // recaptchaTimeLimiter.executeCompletionStage handles scheduling the timeout.
                CompletionStage<GetRecaptchaResponse> timeLimitedStage =
                        recaptchaTimeLimiter.executeCompletionStage(
                                // The scheduler is implicitly used by executeCompletionStage when called on the limiter instance
                                // if the limiter was configured with one, but passing it explicitly is clearer.
                                // However, the instance method doesn't take the scheduler. The static one does.
                                // Let's use the instance method as it's simpler.
                                scheduledExecutorService,
                                asyncOperationSupplier // Pass the supplier that returns the CompletionStage
                        );

                // Block and wait for the result of the time-limited stage.
                // .join() throws CompletionException on underlying errors (including TimeoutException from TimeLimiter).
                return timeLimitedStage.toCompletableFuture().join();

            } catch (CompletionException e) {
                // Unwrap CompletionException to get the actual cause for Resilience4j predicates.
                Throwable cause = e.getCause();
                if (cause instanceof TimeoutException) {
                    log.warn("CAPTCHA validation timed out.");
                    meterRegistry.counter("captcha.validation.timeout").increment();
                    // Rethrow TimeoutException wrapped in RuntimeException for the lambda
                    throw new RuntimeException(cause); // Wrap checked exception
                } else if (cause instanceof RestClientException) {
                    log.warn("CAPTCHA validation failed due to RestClientException: {}", cause.getMessage());
                    // Rethrow RestClientException (which is RuntimeException)
                    throw (RestClientException) cause;
                } else if (cause instanceof Exception) { // Catch other potential checked/unchecked exceptions
                    log.error("Unexpected underlying exception during CAPTCHA validation", cause);
                    // Wrap in RuntimeException for the lambda
                    throw new RuntimeException(cause);
                }
                // If cause is not an Exception or is null, rethrow the original CompletionException
                // wrapped in RuntimeException
                throw new RuntimeException(e);
            } catch (Exception e) { // Catch exceptions during setup/execution before join()
                log.error("Unexpected error during CAPTCHA validation setup/execution", e);
                // Wrap in RuntimeException if it's a checked exception
                throw e; // Rethrow if already RuntimeException
            }
        });


        // Decorate the retrying operation with the Circuit Breaker
        Supplier<GetRecaptchaResponse> resilientSupplier = CircuitBreaker.decorateSupplier(
                recaptchaCircuitBreaker,
                retryingSupplier // Apply Circuit Breaker to the supplier that handles retries and time limits
        );

        // Execute the fully decorated operation
        GetRecaptchaResponse apiResponse;
        try {
            apiResponse = resilientSupplier.get();
        } catch (Exception e) {
            // Catch exceptions thrown after all resilience layers (retries failed, CB open, etc.)
            // This includes the wrapped RuntimeExceptions from the retry lambda.
            log.error("CAPTCHA validation failed after applying resilience policies", e);
            // Rethrow the original exception type if possible, or a generic one
            if (e.getCause() != null) {
                // Attempt to rethrow the original cause
                if (e.getCause() instanceof Exception cause) throw cause;
            }
            // If unwrapping fails, or it wasn't a wrapped exception, throw a generic exception
            throw new CaptchaServiceException("CAPTCHA validation failed after retries/circuit breaker.", e);
        }


        // Process the result if successful
        boolean success = apiResponse != null && apiResponse.success();
        meterRegistry.counter("captcha.validation.api_call",
                "success", String.valueOf(success)).increment();

        if (!success) {
            log.warn("reCAPTCHA validation failed. API Response: {}", apiResponse != null ? apiResponse.errorCodes() : "null response");
        }

        return success;
    }

    @NotNull
    private Supplier<CompletionStage<GetRecaptchaResponse>> getCompletionStageSupplier(String captchaResponse) {
        Supplier<GetRecaptchaResponse> recaptchaApiCallSupplier = () -> {
            MultiValueMap<String, String> requestMap = new LinkedMultiValueMap<>();
            requestMap.add("secret", recaptchaSecret);
            requestMap.add("response", captchaResponse);

            // Make the synchronous API call
            return restTemplate.postForObject(
                    RECAPTCHA_VERIFY_URL,
                    requestMap,
                    GetRecaptchaResponse.class
            );
        };

        // Supplier that creates the asynchronous operation (CompletableFuture)
        // This wraps the synchronous call in an async task executed by the scheduledExecutorService
        // Use the dedicated executor
        return () ->
                CompletableFuture.supplyAsync(
                        recaptchaApiCallSupplier,
                        scheduledExecutorService // Use the dedicated executor
                );
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
        // DO NOT retry RateLimitExceededException or CaptchaRequiredException
    }

    private boolean isRecaptchaRetryableException(Throwable e) {
        // Define which exceptions should trigger a retry for reCAPTCHA API calls
        return e instanceof RestClientException || // Spring's HTTP client exceptions
                e instanceof TimeoutException; // From TimeLimiter
        // DO NOT retry InvalidCaptchaException
    }

    // Fallback method example (if needed for circuit breakers)
    private boolean rateLimitFallback(String clientIp, Throwable t) {
        log.error("Rate limiting fallback triggered for IP: {} due to: {}", clientIp, t.getMessage());
        meterRegistry.counter("captcha.ratelimit.fallback").increment();
        // Decide fail-open or fail-closed
        return true; // Fail-open example
    }

    private boolean captchaValidationFallback(String captchaResponse, Throwable t) {
        log.error("CAPTCHA validation fallback triggered due to: {}", t.getMessage());
        meterRegistry.counter("captcha.validation.fallback").increment();
        // Decide fail-open or fail-closed
        return false; // Fail-closed (treat as invalid)
    }


    // --- Utility Methods ---

    // No changes needed
    private long getCurrentTimestamp() {
        return clock.instant().getEpochSecond();
    }

    /**
     * Gets the approximate remaining rate limit permits for a client IP using Redis Sliding Window.
     * Note: This is an estimate. Calculating the exact remaining count requires another ZCARD call.
     * Returning -1 to indicate that an exact count isn't readily available without another query.
     *
     * @param clientIp The client IP address.
     * @return -1, indicating an exact count is not easily determined without another query.
     */
    public long getRemainingLimit(String clientIp) {
        // Calculating the exact remaining limit for a sliding window requires querying the sorted set size again.
        // For simplicity and performance,
        // we can return -1 or a placeholder indicating it's not directly calculated here.
        // Alternatively, you could execute ZCARD again, but that adds overhead.
        String rateLimitKey = Constants.CAPTCHA_RATELIMIT_KEY + clientIp;
        try {
            return rateLimiter.getRemainingPermits(rateLimitKey);
        } catch (Exception e) {
            log.error("Error getting remaining captcha limit for IP: {}", clientIp, e);
            log.trace("getRemainingLimit is approximate for sliding window. Returning -1 for IP: {}", clientIp);
            return -1; // Indicate not easily calculated or return an approximation if needed.
        }


        /* // Alternative: Query ZCARD again (adds overhead)
        String key = RATE_LIMITER_KEY_PREFIX + clientIp;
        try {
            long nowMillis = clock.instant().toEpochMilli();
            long windowStartMillis = nowMillis - RATE_LIMIT_PERIOD.toMillis();
            // Clean up old entries first (optional, but good practice)
            stringRedisTemplate.opsForZSet().removeRangeByScore(key, Double.NEGATIVE_INFINITY, windowStartMillis);
            // Get current count
            Long currentCount = stringRedisTemplate.opsForZSet().zCard(key);
            long count = (currentCount != null) ? currentCount : 0;
            return Math.max(0, RATE_LIMIT - count);
        } catch (Exception e) {
            log.error("Error getting remaining sliding window limit for IP: {}", clientIp, e);
            return -1; // Indicate error
        }
        */
    }


    // --- Deprecated/Alternative Method ---

    /**
     * @deprecated Use {@link #verifyRateLimitAndCaptcha(String, String)} instead.
     * This method only validates the CAPTCHA response without checking the rate limit first.
     */
    @Deprecated
    public boolean validateCaptcha(String captchaResponse, String clientIp) {
        if (!StringUtils.hasText(captchaResponse)) {
            log.warn("Empty captcha response received from IP: {}", clientIp);
            return false;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Directly call the internal validation logic with resilience
            return validateCaptchaInternal(captchaResponse);
        } catch (Exception e) {
            log.error("Failed to validate captcha for IP: {}", clientIp, e);
            meterRegistry.counter("captcha.validation.error", "context", "direct_call").increment();
            return false;
        } finally {
            sample.stop(meterRegistry.timer("captcha.validation.duration", "context", "direct_call"));
        }
    }

}