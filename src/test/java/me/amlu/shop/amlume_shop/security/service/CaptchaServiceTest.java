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
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CaptchaServiceTest {

    private final RestTemplate restTemplate;
    private final RetryRegistry retryRegistry;
    private RedissonClient redissonClient;
    private MeterRegistry meterRegistry;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private Clock clock; // For handling clock sync issues
    private CaptchaService captchaService;

    CaptchaServiceTest(RestTemplate restTemplate, RetryRegistry retryRegistry, RedissonClient redissonClient, MeterRegistry meterRegistry, CircuitBreaker circuitBreaker, TimeLimiter timeLimiter, Clock clock) {
        this.restTemplate = restTemplate;
        this.retryRegistry = retryRegistry;
        this.redissonClient = redissonClient;
        this.meterRegistry = meterRegistry;
        this.circuitBreaker = circuitBreaker;
        this.timeLimiter = timeLimiter;
        this.clock = clock;
    }

    @BeforeEach
    void setUp() {
        redissonClient = mock(RedissonClient.class);
        meterRegistry = new SimpleMeterRegistry();
        clock = Clock.systemUTC();
        captchaService = new CaptchaService(restTemplate, retryRegistry, redissonClient, meterRegistry, (CircuitBreakerRegistry) circuitBreaker, (TimeLimiterRegistry) timeLimiter, clock);
    }

    @Test
    void shouldReturnRemainingLimit() {
        // Arrange
        RedissonClient redissonClient = mock(RedissonClient.class);
        RRateLimiter rateLimiter = mock(RRateLimiter.class);
        when(redissonClient.getRateLimiter(anyString())).thenReturn(rateLimiter);
        when(rateLimiter.availablePermits()).thenReturn(50L);

//        CaptchaService captchaService = new CaptchaService(redissonClient);

        // Act
        long remainingLimit = captchaService.getRemainingLimit("test-ip");

        // Assert
        assertEquals(50L, remainingLimit);
        verify(rateLimiter).trySetRate(any(), anyLong(), Duration.ofSeconds(anyLong()));
    }

    @Test
    void shouldReturnZeroOnError() {
        // Arrange
        RedissonClient redissonClient = mock(RedissonClient.class);
        when(redissonClient.getRateLimiter(anyString()))
            .thenThrow(new RuntimeException("Connection error"));

//        CaptchaService captchaService = new CaptchaService(redissonClient);

        // Act
        long remainingLimit = captchaService.getRemainingLimit("test-ip");

        // Assert
        assertEquals(0L, remainingLimit);
    }
}
