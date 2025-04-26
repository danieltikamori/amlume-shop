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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.amlu.shop.amlume_shop.exceptions.CaptchaRequiredException;
import me.amlu.shop.amlume_shop.exceptions.InvalidCaptchaException;
import me.amlu.shop.amlume_shop.payload.GetRecaptchaResponse;
import me.amlu.shop.amlume_shop.ratelimiter.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

// Use MockitoExtension for cleaner mock initialization
@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {

    // --- Mocks for Dependencies ---
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private RateLimiter rateLimiter;
    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Mock
    private RetryRegistry retryRegistry;
    @Mock
    private TimeLimiterRegistry timeLimiterRegistry;
    @Mock
    private RedisScript<Long> slidingWindowScript; // Mock the Lua script bean
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ScheduledExecutorService scheduledExecutorService; // Mock the executor

    // --- Mocks for Resilience4j Components ---
    @Mock
    private CircuitBreaker redisCircuitBreaker;
    @Mock
    private Retry redisRetry;
    @Mock
    private CircuitBreaker recaptchaCircuitBreaker;
    @Mock
    private Retry recaptchaRetry;
    @Mock
    private TimeLimiter recaptchaTimeLimiter;

    // --- Concrete Instances ---
    private MeterRegistry meterRegistry;
    private Clock clock;

    // --- Service Under Test ---
    private CaptchaService captchaService;

    // --- Constants for Resilience4j Names ---
    private static final String REDIS_CIRCUIT_BREAKER = "redisCircuitBreaker";
    private static final String REDIS_RETRY = "redisRetry";
    private static final String RECAPTCHA_CIRCUIT_BREAKER = "recaptchaCircuitBreaker";
    private static final String RECAPTCHA_RETRY = "recaptchaRetry";
    private static final String RECAPTCHA_TIME_LIMITER = "recaptchaTimeLimiter";

    @BeforeEach
    void setUp() {
        // Initialize concrete instances
        meterRegistry = new SimpleMeterRegistry();
        clock = Clock.systemUTC(); // Use a fixed clock for predictable timestamp-based tests if needed

        // Configure mock registries to return mock components
        when(circuitBreakerRegistry.circuitBreaker(REDIS_CIRCUIT_BREAKER)).thenReturn(redisCircuitBreaker);
        when(retryRegistry.retry(REDIS_RETRY)).thenReturn(redisRetry);
        when(circuitBreakerRegistry.circuitBreaker(RECAPTCHA_CIRCUIT_BREAKER)).thenReturn(recaptchaCircuitBreaker);
        when(retryRegistry.retry(RECAPTCHA_RETRY)).thenReturn(recaptchaRetry);
        when(timeLimiterRegistry.timeLimiter(RECAPTCHA_TIME_LIMITER)).thenReturn(recaptchaTimeLimiter);

        // --- Mock Resilience4j Decorators ---
        // Mock the decorators to simply execute the underlying supplier for basic tests.
        // More complex tests might mock specific behaviors (e.g., throwing exceptions).

        // Redis Retry/CircuitBreaker
        when(redisRetry.executeSupplier(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        when(redisCircuitBreaker.executeSupplier(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // Recaptcha Retry/CircuitBreaker/TimeLimiter
        // Mock TimeLimiter to execute the CompletionStage immediately
        when(recaptchaTimeLimiter.executeCompletionStage(any(ScheduledExecutorService.class), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<CompletionStage<?>> supplier = invocation.getArgument(1);
                    return supplier.get(); // Execute the supplier which returns the CompletionStage
                });

        // Mock Retry for Recaptcha (handles the timestamp limiter execution within its lambda)
        when(recaptchaRetry.executeSupplier(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        // Mock Circuit Breaker for Recaptcha
        when(recaptchaCircuitBreaker.executeSupplier(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });


        // Instantiate the service with all mocks and concrete instances
        captchaService = new CaptchaService(
                restTemplate,
                rateLimiter,
                meterRegistry,
                circuitBreakerRegistry,
                retryRegistry,
                timeLimiterRegistry,
                clock,
                // Pass the mocked executor
                scheduledExecutorService
        );

        // Set the recaptcha secret (can be done via reflection or making it package-private for testing)
        // Or preferably, use constructor injection if possible, or a setter.
        // For simplicity here, we assume it's set via @Value, which works in integration tests
        // but needs manual setting or reflection in unit tests if not using constructor injection.
        // Let's assume it's handled or not critical for these specific unit tests.
        // ReflectionTestUtils.setField(captchaService, "recaptchaSecret", "test-secret");
    }

    // --- Test Cases ---

    @Test
    void verifyRateLimitAndCaptcha_WhenRateLimitOk_ShouldSucceed() throws Exception {
        // Arrange: Mock Redis Lua script to return 1 (permit acquired)
        when(stringRedisTemplate.execute(eq(slidingWindowScript), any(List.class), any(String.class)))
                .thenReturn(1L);

        // Act & Assert: Call the method and expect no exception
        assertDoesNotThrow(() -> captchaService.verifyRateLimitAndCaptcha("1.2.3.4", null));

        // Verify: Ensure Redis script was called, but RestTemplate (Captcha validation) was NOT
        verify(stringRedisTemplate).execute(eq(slidingWindowScript), any(List.class), any());
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    void verifyRateLimitAndCaptcha_WhenRateLimitExceededAndNoCaptcha_ShouldThrowCaptchaRequiredException() {
        // Arrange: Mock Redis Lua script to return 0 (permit denied)
        when(stringRedisTemplate.execute(eq(slidingWindowScript), any(List.class), any()))
                .thenReturn(0L);

        // Act & Assert: Expect CaptchaRequiredException
        assertThrows(CaptchaRequiredException.class, () ->
                captchaService.verifyRateLimitAndCaptcha("1.2.3.4", null)
        );

        // Verify: Ensure Redis script was called, but RestTemplate was NOT
        verify(stringRedisTemplate).execute(eq(slidingWindowScript), any(List.class), any());
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    void verifyRateLimitAndCaptcha_WhenRateLimitExceededAndInvalidCaptcha_ShouldThrowInvalidCaptchaException() {
        // Arrange: Mock Redis Lua script to return 0 (permit denied)
        when(stringRedisTemplate.execute(eq(slidingWindowScript), any(List.class), any()))
                .thenReturn(0L);
        // Arrange: Mock RestTemplate to return failure response
        GetRecaptchaResponse failureResponse = new GetRecaptchaResponse(false, null, null, Collections.singletonList("invalid-input-response"));
        // Mock the async execution result within the TimeLimiter/Retry structure
        when(restTemplate.postForObject(anyString(), any(), eq(GetRecaptchaResponse.class)))
                .thenReturn(failureResponse);

        // Act & Assert: Expect InvalidCaptchaException
        assertThrows(InvalidCaptchaException.class, () ->
                captchaService.verifyRateLimitAndCaptcha("1.2.3.4", "invalid-captcha")
        );

        // Verify: Ensure both Redis script and RestTemplate were called
        verify(stringRedisTemplate).execute(eq(slidingWindowScript), any(List.class), any());
        verify(restTemplate).postForObject(anyString(), any(), eq(GetRecaptchaResponse.class));
    }

    @Test
    void verifyRateLimitAndCaptcha_WhenRateLimitExceededAndValidCaptcha_ShouldSucceed() throws Exception {
        // Arrange: Mock Redis Lua script to return 0 (permit denied)
        when(stringRedisTemplate.execute(eq(slidingWindowScript), any(List.class), any()))
                .thenReturn(0L);
        // Arrange: Mock RestTemplate to return success response
        GetRecaptchaResponse successResponse = new GetRecaptchaResponse(true, null, null, null);
        // Mock the async execution result
        when(restTemplate.postForObject(anyString(), any(), eq(GetRecaptchaResponse.class)))
                .thenReturn(successResponse);

        // Act & Assert: Expect no exception
        assertDoesNotThrow(() ->
                captchaService.verifyRateLimitAndCaptcha("1.2.3.4", "valid-captcha")
        );

        // Verify: Ensure both Redis script and RestTemplate were called
        verify(stringRedisTemplate).execute(eq(slidingWindowScript), any(List.class), any());
        verify(restTemplate).postForObject(anyString(), any(), eq(GetRecaptchaResponse.class));
    }

    @Test
    void verifyRateLimitAndCaptcha_WhenRedisCheckFails_ShouldSucceed ( ) throws Exception {
        // Arrange: Mock Redis Lua script execution to throw an exception
        when(stringRedisTemplate.execute(eq(slidingWindowScript), any(List.class), any(String.class)))
                .thenThrow(new RuntimeException("Redis connection error")); // Simulate Redis failure

        // Act & Assert: Expect no exception (fail-open behavior)
        assertDoesNotThrow(() -> captchaService.verifyRateLimitAndCaptcha("1.2.3.4", null));

        // Verify: Ensure Redis script was attempted, but RestTemplate was NOT called
        verify(stringRedisTemplate).execute(eq(slidingWindowScript), any(List.class), any());
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    void verifyRateLimitAndCaptcha_WhenCaptchaValidationFails_ShouldThrowInvalidCaptchaException() {
        // Arrange: Mock Redis Lua script to return 0 (permit denied)
        when(stringRedisTemplate.execute(eq(slidingWindowScript), any(List.class), any(String.class)))
                .thenReturn(0L);
        // Arrange: Mock RestTemplate call to throw an exception
        when(restTemplate.postForObject(anyString(), any(), eq(GetRecaptchaResponse.class)))
                .thenThrow(new RestClientException("Google API error"));

        // Act & Assert: Expect InvalidCaptchaException
        assertThrows(InvalidCaptchaException.class, () ->
                captchaService.verifyRateLimitAndCaptcha("1.2.3.4", "any-captcha")
        );

        // Verify: Ensure both Redis script and RestTemplate were called (or attempted)
        verify(stringRedisTemplate).execute(eq(slidingWindowScript), any(List.class), any());
        verify(restTemplate).postForObject(anyString(), any(), eq(GetRecaptchaResponse.class));
    }

    @Test
    void verifyRateLimitAndCaptcha_WhenCaptchaValidationTimesOut_ShouldThrowInvalidCaptchaException() throws Exception {
        // Arrange: Mock Redis Lua script to return 0 (permit denied)
        when(stringRedisTemplate.execute(eq(slidingWindowScript), any(List.class), any(String.class)))
                .thenReturn(0L);

        // Arrange: Mock TimeLimiter to throw TimeoutException
        // We need to mock the behavior within the retry lambda.
        // Let's adjust the retry mock to simulate the CompletionException wrapping TimeoutException.
        when(recaptchaRetry.executeSupplier(any())).thenAnswer(invocation -> {
            // Simulate the behavior where the timeLimitedStage.join() throws CompletionException(TimeoutException)
            throw new RuntimeException(new TimeoutException("CAPTCHA validation timed out"));
        });

        // Act & Assert: Expect InvalidCaptchaException
        assertThrows(InvalidCaptchaException.class, () ->
                captchaService.verifyRateLimitAndCaptcha("1.2.3.4", "any-captcha")
        );

        // Verify: Redis script was called. RestTemplate might have been called within the async task
        // before the timeout occurred, but the overall operation failed due to timeout.
        verify(stringRedisTemplate).execute(eq(slidingWindowScript), any(List.class), any());
        // Verification of restTemplate call depends on *when* the timeout is simulated.
        // If timeout happens before the call, it won't be called. If after, it might be.
        // For simplicity, we can omit verification or verify with atMost(1).
        // verify(restTemplate, atMost(1)).postForObject(anyString(), any(), eq(RecaptchaResponse.class));
    }


    @Test
    void getRemainingLimit_ShouldReturnMinusOne() {
        // Arrange: No specific arrangements needed as the method currently returns a fixed value.

        // Act
        long remainingLimit = captchaService.getRemainingLimit("test-ip");

        // Assert
        assertEquals(-1L, remainingLimit, "getRemainingLimit should return -1 for sliding window");
    }

    // Removed the old Redisson-based tests as they are no longer relevant
    /*
    @Test
    void shouldReturnRemainingLimit() { ... }

    @Test
    void shouldReturnZeroOnError() { ... }
    */
}