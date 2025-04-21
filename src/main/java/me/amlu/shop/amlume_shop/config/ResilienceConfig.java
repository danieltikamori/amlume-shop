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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Configuration class for Resilience4j components and related beans.
 * Provides configurations for Circuit Breaker, Time Limiter, Retry, Bulkhead, and other resilience patterns.
 */
@Configuration
public class ResilienceConfig {

    /**
     * Configures a RestTemplate bean with custom connection and read timeouts.
     *
     * @return a configured RestTemplate instance.
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000); // 5 seconds
        requestFactory.setReadTimeout(10000); // 10 seconds
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }

    /**
     * Configures a CircuitBreakerRegistry bean with custom settings.
     *
     * @return a CircuitBreakerRegistry instance.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Circuit opens if 50% of calls fail.
                .waitDurationInOpenState(Duration.ofSeconds(20)) // Wait 20 seconds before transitioning to half-open state.
                .permittedNumberOfCallsInHalfOpenState(5) // Allow 5 calls in half-open state.
                .slidingWindowSize(10) // Use a sliding window of 10 calls for metrics.
                .build();

        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    /**
     * Configures a TimeLimiterRegistry bean with custom timeout settings.
     *
     * @return a TimeLimiterRegistry instance.
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(1)) // Set timeout duration to 1 second
                .build();
        return TimeLimiterRegistry.of(config);
    }

    /**
     * Configures a RetryRegistry bean with custom retry settings.
     *
     * @return a RetryRegistry instance.
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(5) // Retry up to 5 times
                .waitDuration(Duration.ofMillis(200)) // Wait initially 200 ms between retries (exponential backoff)
                .intervalFunction(IntervalFunction.ofExponentialBackoff()) // Use exponential backoff for retry intervals.
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
     * Configures a BulkheadRegistry bean with custom settings for concurrent calls.
     *
     * @return a BulkheadRegistry instance.
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(100) // Allow up to 100 concurrent calls
                .maxWaitDuration(Duration.ofMillis(300)) // Wait up to 300 ms for a free slot
                .build();
        return BulkheadRegistry.of(config);
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
        return Executors.newScheduledThreadPool(4); // Example: pool size of 4
    }
}

