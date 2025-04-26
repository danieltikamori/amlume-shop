/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.config;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Configuration class for Resilience4j components and related beans.
 * Provides configurations for Circuit Breaker, Time Limiter, Retry, Bulkhead, and other resilience patterns.
 */
@Configuration
public class ResilienceConfig {

@Value("${resilience4j.rest-template.connect-timeout:5000}")
    public int CONNECT_TIMEOUT;
@Value("${resilience4j.rest-template.read-timeout:10000}")
    public int READ_TIMEOUT;
    
    // --- Bulkhead constants ---
    @Value("${resilience4j.bulkhead.instances.default.max-concurrent-calls:100}")
    public int MAX_CONCURRENT_CALLS;
    @Value("${resilience4j.bulkhead.max-wait-duration:300}")
    public Duration MAX_WAIT_DURATION;

    // --- Circuit breaker constants ---
    @Value("${resilience4j.circuitbreaker.instances.default.failure-rate-threshold:50}")
    public float FAILURE_RATE_THRESHOLD;

    @Value("${resilience4j.circuitbreaker.instances.default.wait-duration-in-open-state:20}")
    public Duration WAIT_DURATION_IN_OPEN_STATE;

    @Value("${resilience4j.circuitbreaker.instances.default.permitted-number-of-calls-in-half-open-state:5}")
    public int PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE;

    @Value("${resilience4j.circuitbreaker.instances.default.sliding-window-size:100}")
    public int DEFAULT_CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE = MAX_CONCURRENT_CALLS;

    // --- Rate limiter constants ---

    // --- Retry constants ---
    @Value("${resilience4j.retry.instances.default.max-attempts:3}")
    public int MAX_RETRY_ATTEMPTS;

    @Value("${resilience4j.retry.instances.default.max-requests-per-minute:100L}")
    public Long MAX_REQUESTS_PER_MINUTE;

    @Value("${resilience4j.retry.instances.default.retry-interval:1000L}")
    public long RETRY_INTERVAL;

    @Value("${resilience4j.retry.instances.default.retry-wait-duration:200}")
    @DurationUnit(ChronoUnit.MILLIS) // Specify unit if property is just a number (e.g., 500)
    public Duration RETRY_WAIT_DURATION;

    // --- Exponential backoff constants ---
    @Value("${resilience4j.exponential-backoff.instances.default.initial-interval-millis:200}")
    private long INITIAL_INTERVAL_MILLIS;

    @Value("${resilience4j.exponential-backoff.instances.default.eb-multiplier:1.5}")
    private double EB_MULTIPLIER;

    @Value("${resilience4j.exponential-backoff.instances.default.randomization-factor:0.36}")
    private double RANDOMIZATION_FACTOR;

    @Value("${resilience4j.exponential-backoff.instances.default.max-interval-millis:86400000L}")
    private long MAX_INTERVAL_MILLIS;

    // --- Time limiter constants ---
    @Value("${resilience4j.time-limiter.instances.default.timeout-duration:1}")
    @DurationUnit(ChronoUnit.SECONDS) // Specify unit if property is just a number (e.g., 500)
    public Duration TIMEOUT_DURATION;

    // --- Executor constants ---
    @Value("${resilience4j.executor.instances.default.core-pool-size:4}")
    public int CORE_POOL_SIZE;

    /**
     * Configures a RestTemplate bean with custom connection and read timeouts.
     *
     * @return a configured RestTemplate instance.
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT); // 5 seconds
        requestFactory.setReadTimeout(READ_TIMEOUT); // 10 seconds
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }

    /**
     * Configures a BulkheadRegistry bean with custom settings for concurrent calls.
     *
     * @return a BulkheadRegistry instance.
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(MAX_CONCURRENT_CALLS) // Allow up to 100 concurrent calls
                .maxWaitDuration(MAX_WAIT_DURATION) // Wait up to 300 ms for a free slot
                .build();
        return BulkheadRegistry.of(config);
    }

    /**
     * Configures a CircuitBreakerRegistry bean with custom settings.
     *
     * @return a CircuitBreakerRegistry instance.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(FAILURE_RATE_THRESHOLD) // The Circuit opens if 50% of calls fail.
                .waitDurationInOpenState(WAIT_DURATION_IN_OPEN_STATE) // Wait 20 seconds before transitioning to half-open state.
                .permittedNumberOfCallsInHalfOpenState(PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE) // Allow 5 calls in half-open state.
                .slidingWindowSize(DEFAULT_CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE) // Use a sliding window of 100 calls for metrics.
                .build();

        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    /**
     * Configures a RetryRegistry bean with custom retry settings.
     *
     * @return a RetryRegistry instance.
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE) // Retry up to 5 times
                .waitDuration(RETRY_WAIT_DURATION) // Wait initially 200 ms between retries
                // If the client is a real person,
                // evaluate exponential backoff is really necessary (critical use cases where reliability is a must).
                // For non-human clients, like application, exponential backoff may make sense.
//                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
//                        INITIAL_INTERVAL_MILLIS, EB_MULTIPLIER,RANDOMIZATION_FACTOR, MAX_INTERVAL_MILLIS)
//                ) // Use exponential backoff with Jittered (randomness) IntervalFunction to avoid collisions for retry intervals.
//                .retryExceptions(RestClientException.class) // Simpler retry on specific exceptions
                // t.getMessage() may throw NullPointerException if the message is null, so always check for null
                // Simplify and avoid potential runtime errors by just handling specific exceptions
                .retryOnException(t -> t instanceof RestClientException || (t instanceof SocketTimeoutException /*&& t.getMessage() != null && t.getMessage().toLowerCase().contains("timeout")*/)) // Retry on specific exceptions and conditions
                // Optional: Customize retry conditions further
                 .retryOnResult(Objects::isNull) // Retry on a specific result
                // .retryOnPredicate(Predicate.not(response -> response.getStatusCode().is2xxSuccessful())) // Retry on non-2xx status codes
                .build();
        return RetryRegistry.of(retryConfig);
    }

    /**
     * Configures a TimeLimiterRegistry bean with custom timeout settings.
     *
     * @return a TimeLimiterRegistry instance.
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(TIMEOUT_DURATION) // Set timeout duration to 1 second
                .build();
        return TimeLimiterRegistry.of(config);
    }

    /**
     * Configures a ScheduledExecutorService bean for managing timeouts on captcha validation calls.
     *
     * @return a ScheduledExecutorService instance with a thread pool size of 4.
     */
    @Bean(name = "captchaTimeLimiterExecutor") // Use the specific name required by the @Qualifier
    public ScheduledExecutorService captchaTimeLimiterExecutor() {
        // Choose an appropriate pool size.
        // Start with a small number and monitor.
        // This pool is specifically for managing timeouts on captcha validation calls.
        return Executors.newScheduledThreadPool(CORE_POOL_SIZE); // Example: pool size of 4
    }
}

