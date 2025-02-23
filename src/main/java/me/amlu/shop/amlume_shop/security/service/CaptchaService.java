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
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.lettuce.core.cluster.PartitionException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.payload.RecaptchaResponse;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Service
public class CaptchaService {
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final String RATE_LIMITER_KEY = "captcha:ratelimit:";
    private static final int RATE_LIMIT = 100; // requests
    private static final int RATE_LIMIT_PERIOD = 3600; // seconds (1 hour)

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(100);


    @Value("${recaptcha.secret}")
    private String recaptchaSecret;

    private final RestTemplate restTemplate;
    private final RetryRegistry retryRegistry;
    private final RedissonClient redissonClient;
    private final MeterRegistry meterRegistry;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final Clock clock; // For handling clock sync issues

    public CaptchaService(
            RestTemplate restTemplate,
            RetryRegistry retryRegistry,
            RedissonClient redissonClient,
            MeterRegistry meterRegistry, CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry, Clock clock) {
        this.restTemplate = restTemplate;
        this.retryRegistry = retryRegistry;
        this.redissonClient = redissonClient;
        this.meterRegistry = meterRegistry;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("valkeyService");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("valkeyService");
        this.clock = clock;
    }

    public boolean validateCaptcha(String captchaResponse, String clientIp) {
        if (!StringUtils.hasText(captchaResponse)) {
            log.warn("Empty captcha response received from IP: {}", clientIp);
            return false;
        }

        // Rate limiting check
        if (!checkRateLimit(clientIp)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            meterRegistry.counter("captcha.ratelimit.exceeded").increment();
            throw new RateLimitExceededException("Too many captcha validation attempts");
        }

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            return retryRegistry.retry("captchaValidation")
                    .executeSupplier(() -> performCaptchaValidation(captchaResponse));

        } catch (Exception e) {
            log.error("Failed to validate captcha for IP: {}", clientIp, e);
            meterRegistry.counter("captcha.validation.error").increment();
            return false;
        } finally {
            sample.stop(meterRegistry.timer("captcha.validation.duration"));
        }
    }

    private boolean performCaptchaValidation(String captchaResponse) {
        MultiValueMap<String, String> requestMap = new LinkedMultiValueMap<>();
        requestMap.add("secret", recaptchaSecret);
        requestMap.add("response", captchaResponse);

        RecaptchaResponse apiResponse = restTemplate.postForObject(
                RECAPTCHA_VERIFY_URL,
                requestMap,
                RecaptchaResponse.class
        );

        boolean success = apiResponse != null && apiResponse.isSuccess();
        meterRegistry.counter("captcha.validation",
                "success", String.valueOf(success)).increment();

        return success;
    }

    private boolean checkRateLimit(String clientIp) {
        return Try.of(() -> {
            return CircuitBreaker.decorateSupplier(circuitBreaker,
                            () -> performRateLimitCheck(clientIp))
                    .get();
        }).recover(Exception.class, e -> {
            log.error("Rate limiting failed, falling back to default behavior", e);
            meterRegistry.counter("ratelimit.fallback").increment();
            return true; // or false based on your security requirements
        }).get();
    }

    private boolean performRateLimitCheck(String clientIp) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(RATE_LIMITER_KEY + clientIp);
        rateLimiter.trySetRate(RateType.OVERALL, RATE_LIMIT, Duration.ofSeconds(RATE_LIMIT_PERIOD));
        return rateLimiter.tryAcquire(1);
    }

    private boolean handleValkeyOperation(String clientIp, Supplier<Boolean> operation) {
        try {
            return Retry.decorateSupplier(
                    Retry.of("valkeyOperation", RetryConfig.custom()
                            .maxAttempts(MAX_RETRY_ATTEMPTS)
                            .waitDuration(RETRY_BACKOFF)
                            .retryOnException(this::isRetryableException)
                            .build()),
                    () -> {
                        try {
                            return operation.get();
                        } catch (RedissonConnectionException e) {
                            handleConnectionFailure(clientIp, e);
                            throw e;
                        } catch (RedissonTimeoutException e) {
                            handleTimeout(clientIp, e);
                            throw e;
                        }
                    }
            ).get();
        } catch (Exception e) {
            return handleFailure(clientIp, e);
        }
    }

    private boolean handleFailure(String clientIp, Exception e) {
        log.error("Operation failed for IP: {} after {} retries", clientIp, MAX_RETRY_ATTEMPTS, e);
        meterRegistry.counter("valkey.operation.failure",
                "type", e.getClass().getSimpleName()).increment();
        return fallbackBehavior(clientIp);
    }

    private boolean fallbackBehavior(String clientIp) {
        log.error("Fallback strategy invoked for IP: {}", clientIp);
        //TODO: Implement fallback strategy
        return true; // True if successful or false based on your security requirements
    }

    private void handleConnectionFailure(String clientIp, RedissonConnectionException e) {
        log.error("Connection failure for IP: {}", clientIp, e);
        meterRegistry.counter("valkey.operation.failure",
                "type", e.getClass().getSimpleName()).increment();
    }

    private void handleTimeout(String clientIp, RedissonTimeoutException e) {
        log.error("Timeout for IP: {}", clientIp, e);
        meterRegistry.counter("valkey.timeout").increment();
    }

    private boolean isRetryableException(Throwable e) {
        return e instanceof RedissonConnectionException ||
                e instanceof RedissonTimeoutException ||
                e instanceof PartitionException ||
                e instanceof InterruptedException ||
                e instanceof RateLimitException ||
                e instanceof TooManyAttemptsException;
    }

    private long getCurrentTimestamp() {
        return clock.instant().getEpochSecond();
    }

    public long getRemainingLimit(String clientIp) {
        try {
            RRateLimiter rateLimiter = redissonClient.getRateLimiter(RATE_LIMITER_KEY + clientIp);
            rateLimiter.trySetRate(RateType.OVERALL, RATE_LIMIT, Duration.ofSeconds(RATE_LIMIT_PERIOD));

            // Get the remaining available permits
            return rateLimiter.availablePermits();
        } catch (Exception e) {
            log.error("Error getting remaining rate limit for IP: {}", clientIp, e);
            return 0; // Return 0 as a safe default
        }
    }
}