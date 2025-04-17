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
import me.amlu.shop.amlume_shop.exceptions.CaptchaRequiredException;
import me.amlu.shop.amlume_shop.exceptions.CaptchaServiceException;
import me.amlu.shop.amlume_shop.exceptions.InvalidCaptchaException;
import me.amlu.shop.amlume_shop.exceptions.RateLimitExceededException;
import me.amlu.shop.amlume_shop.payload.RecaptchaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final String RATE_LIMITER_KEY_PREFIX = "captcha:ratelimit:sliding"; // Added prefix for clarity
    private static final long RATE_LIMIT = 100; // requests per period
    private static final Duration RATE_LIMIT_PERIOD = Duration.ofHours(1); // Use Duration

    // Removed MAX_RETRY_ATTEMPTS and RETRY_BACKOFF as they are configured in Resilience4j beans

    // Resilience4j instance names (ensure these match your configuration)
    private static final String REDIS_CIRCUIT_BREAKER = "redisCircuitBreaker";
    private static final String REDIS_RETRY = "redisRetry";
    private static final String RECAPTCHA_CIRCUIT_BREAKER = "recaptchaCircuitBreaker";
    private static final String RECAPTCHA_RETRY = "recaptchaRetry";
    private static final String RECAPTCHA_TIME_LIMITER = "recaptchaTimeLimiter";


    @Value("${recaptcha.secret}")
    private String recaptchaSecret;

    private final RestTemplate restTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final MeterRegistry meterRegistry;
    private final CircuitBreaker redisCircuitBreaker;
    private final Retry redisRetry;
    private final CircuitBreaker recaptchaCircuitBreaker;
    private final Retry recaptchaRetry;
    private final TimeLimiter recaptchaTimeLimiter;
    private final Clock clock; // For handling clock sync issues

    public CaptchaService(
            RestTemplate restTemplate,
            StringRedisTemplate stringRedisTemplate,
            MeterRegistry meterRegistry,
            // Qualify registries if multiple beans exist
            @Qualifier("circuitBreakerRegistry") CircuitBreakerRegistry circuitBreakerRegistry,
            @Qualifier("retryRegistry") RetryRegistry retryRegistry,
            @Qualifier("timeLimiterRegistry") TimeLimiterRegistry timeLimiterRegistry,
            Clock clock) {
        this.restTemplate = restTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.meterRegistry = meterRegistry;
        // Get or create specific instances from registries
        this.redisCircuitBreaker = circuitBreakerRegistry.circuitBreaker(REDIS_CIRCUIT_BREAKER);
        this.redisRetry = retryRegistry.retry(REDIS_RETRY);
        this.recaptchaCircuitBreaker = circuitBreakerRegistry.circuitBreaker(RECAPTCHA_CIRCUIT_BREAKER);
        this.recaptchaRetry = retryRegistry.retry(RECAPTCHA_RETRY);
        this.recaptchaTimeLimiter = timeLimiterRegistry.timeLimiter(RECAPTCHA_TIME_LIMITER);
        this.clock = clock;
    }

    /**
     * Validates the rate limit for a client IP. If the limit is exceeded,
     * it then validates the provided CAPTCHA response.
     *
     * @param clientIp        The IP address of the client.
     * @param captchaResponse The CAPTCHA response string from the client (can be null initially).
     * @throws RateLimitExceededException If rate limit is hit AND CAPTCHA is required but not provided.
     * @throws InvalidCaptchaException    If rate limit is hit AND the provided CAPTCHA is invalid.
     * @throws CaptchaServiceException    If an unexpected error occurs during validation.
     */
    public void verifyRateLimitAndCaptcha(String clientIp, String captchaResponse)
            throws RateLimitExceededException, InvalidCaptchaException, CaptchaServiceException {

        boolean permitAcquired;
        try {
            // 1. Check Rate Limit
            permitAcquired = tryAcquireRateLimitPermit(clientIp);

        } catch (Exception e) {
            // Handle failure during rate limit check (e.g., Redis connection issue)
            log.error("Rate limiting check failed for IP: {}. Allowing request (fail-open).", clientIp, e);
            meterRegistry.counter("captcha.ratelimit.check.error").increment();
            // Fail-open: Allow the request if rate limiting check fails.
            // Alternatively, you could fail-closed by throwing an exception here.
            return; // Allow request
        }

        if (permitAcquired) {
            // Rate limit OK, no CAPTCHA needed for this request
            log.trace("Rate limit check passed for IP: {}", clientIp);
            return; // Success
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
                captchaIsValid = validateCaptchaInternal(captchaResponse);
            } catch (Exception e) {
                // Handle failure during CAPTCHA validation (e.g., Google API error)
                log.error("CAPTCHA validation failed for IP: {} after rate limit hit.", clientIp, e);
                meterRegistry.counter("captcha.validation.error", "context", "after_rate_limit").increment();
                // Treat validation error as invalid CAPTCHA for security
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
     * Tries to acquire a permit from the rate limiter for the given client IP using Redis INCR/EXPIRE.
     * Applies Resilience4j Circuit Breaker and Retry.
     *
     * @param clientIp The client IP address.
     * @return true if a permit was acquired (count <= limit), false if the rate limit was hit (count > limit).
     * @throws Exception If an error occurs during the Redis operation after retries.
     */
    private boolean tryAcquireRateLimitPermit(String clientIp) throws Exception {
        Supplier<Boolean> rateLimitCheckSupplier = () -> {
            String key = RATE_LIMITER_KEY_PREFIX + clientIp;
            try {
                // Increment the counter for the IP address
                Long currentCount = stringRedisTemplate.opsForValue().increment(key);

                if (currentCount == null) {
                    // This shouldn't happen normally with increment, but handle defensively
                    log.error("Redis increment returned null for key: {}. Assuming failure.", key);
                    throw new CaptchaServiceException("Failed to increment rate limit counter in Redis.");
                }

                // If this is the first request for this IP in the current window, set the expiry
                if (currentCount == 1) {
                    Boolean expired = stringRedisTemplate.expire(key, RATE_LIMIT_PERIOD);
                    if (Boolean.FALSE.equals(expired)) {
                        // Log if expire fails, but proceed. The key will eventually expire.
                        log.warn("Failed to set expiry for rate limit key: {}", key);
                    }
                }

                // Check if the current count exceeds the limit
                return currentCount <= RATE_LIMIT;

            } catch (DataAccessException e) {
                // Catch Spring Data Redis exceptions
                log.error("Redis error during rate limit check for key: {}", key, e);
                throw new CaptchaServiceException("Redis error during rate limit check", e);
            }
        };

        // Decorate with Retry and Circuit Breaker (using the renamed instances)
        Supplier<Boolean> resilientSupplier = Retry.decorateSupplier(redisRetry,
                CircuitBreaker.decorateSupplier(redisCircuitBreaker, rateLimitCheckSupplier)
        );

        // Execute the resilient operation
        return resilientSupplier.get();
    }


    /**
     * Performs the actual CAPTCHA validation by calling the Google reCAPTCHA API.
     * Applies Resilience4j Circuit Breaker, Retry, and Time Limiter.
     *
     * @param captchaResponse The CAPTCHA response string.
     * @return true if the CAPTCHA is valid, false otherwise.
     * @throws Exception If the validation fails after retries or due to circuit breaker/timeout.
     */
    private boolean validateCaptchaInternal(String captchaResponse) throws Exception {
        Supplier<RecaptchaResponse> recaptchaApiCallSupplier = () -> {
            MultiValueMap<String, String> requestMap = new LinkedMultiValueMap<>();
            requestMap.add("secret", recaptchaSecret);
            requestMap.add("response", captchaResponse);

            // Make the API call
            return restTemplate.postForObject(
                    RECAPTCHA_VERIFY_URL,
                    requestMap,
                    RecaptchaResponse.class
            );
        };

        // Decorate with TimeLimiter, Retry, and Circuit Breaker
        Supplier<CompletionStage<RecaptchaResponse>> timeLimitedSupplier =
                recaptchaTimeLimiter.decorateCompletionStageSupplier(() ->
                        java.util.concurrent.CompletableFuture.supplyAsync(recaptchaApiCallSupplier)
                );

        Supplier<RecaptchaResponse> resilientSupplier = Retry.decorateSupplier(recaptchaRetry,
                CircuitBreaker.decorateSupplier(recaptchaCircuitBreaker,
                        () -> timeLimitedSupplier.get().toCompletableFuture().join() // Adapt CompletionStage to Supplier
                )
        );

        // Execute and process the result
        RecaptchaResponse apiResponse = resilientSupplier.get();

        boolean success = apiResponse != null && apiResponse.isSuccess();
        meterRegistry.counter("captcha.validation.api_call",
                "success", String.valueOf(success)).increment();

        if (!success) {
            log.warn("reCAPTCHA validation failed. API Response: {}", apiResponse != null ? apiResponse.getErrorCodes() : "null response");
        }

        return success;
    }


    // --- Helper methods for Resilience4j (Retry predicate, Fallback, etc.) ---

    private boolean isRetryableException(Throwable e) {
        // Define which exceptions should trigger a retry for Redis operations
        return e instanceof RedisConnectionFailureException || // Spring Data Redis connection exception
                // e instanceof RedissonTimeoutException || // Removed Redisson exception
                e instanceof DataAccessException || // Broader Spring Data exception
                e instanceof PartitionException || // Lettuce cluster exception
                e instanceof InterruptedException;
        // DO NOT retry RateLimitExceededException or CaptchaRequiredException
    }

    // isRecaptchaRetryableException remains the same

    private boolean isRecaptchaRetryableException(Throwable e) {
        // Define which exceptions should trigger a retry for reCAPTCHA API calls
        return e instanceof RestClientException || // Spring's HTTP client exceptions
                e instanceof java.util.concurrent.TimeoutException; // From TimeLimiter
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
        return false; // Fail-closed example (treat as invalid)
    }


    // --- Utility Methods ---

    private long getCurrentTimestamp() {
        return clock.instant().getEpochSecond();
    }

    /**
     * Gets the approximate remaining rate limit permits for a client IP using Redis.
     * Note: This is an estimate based on the current count within the fixed window.
     *
     * @param clientIp The client IP address.
     * @return The approximate number of remaining permits, or RATE_LIMIT if the key doesn't exist or an error occurs.
     */
    public long getRemainingLimit(String clientIp) {
        String key = RATE_LIMITER_KEY_PREFIX + clientIp;
        try {
            // Get the current count
            String countStr = stringRedisTemplate.opsForValue().get(key);
            long currentCount = (countStr != null) ? Long.parseLong(countStr) : 0;

            // Calculate remaining permits
            long remaining = RATE_LIMIT - currentCount;

            // Ensure remaining is not negative
            return Math.max(0, remaining);

        } catch (NumberFormatException e) {
            log.error("Invalid number format in Redis for rate limit key: {}", key, e);
            return 0; // Treat format error as limit reached
        } catch (DataAccessException e) {
            log.error("Error getting remaining rate limit from Redis for IP: {}", clientIp, e);
            return RATE_LIMIT; // Fail-open: Assume full limit available on error
        } catch (Exception e) {
            log.error("Unexpected error getting remaining rate limit for IP: {}", clientIp, e);
            return RATE_LIMIT; // Fail-open
        }
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

    // --- Removed unused methods ---
    // private boolean handleValkeyOperation(...)
    // private boolean handleFailure(...)
    // private boolean fallbackBehavior(...)
    // private void handleConnectionFailure(...)
    // private void handleTimeout(...)
}