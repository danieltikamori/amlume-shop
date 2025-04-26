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
import me.amlu.shop.amlume_shop.resilience.properties.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Configuration class for Resilience4j components and related beans.
 * Provides configurations for Circuit Breaker, Time Limiter, Retry, Bulkhead, and other resilience patterns.
 */
@Configuration
@EnableConfigurationProperties({Resilience4jBulkheadProperties.class,
        Resilience4jCircuitBreakerProperties.class,
        Resilience4jExecutorProperties.class,
        Resilience4jExponentialBackoffProperties.class,
        Resilience4jRetryProperties.class,
        Resilience4jTimeLimiterProperties.class,
        RestTemplateProperties.class
})
public class ResilienceConfig {

    private final RestTemplateProperties restTemplateProperties;
    private final Resilience4jBulkheadProperties bulkheadProperties;
    private final Resilience4jCircuitBreakerProperties circuitBreakerProperties;
    private final Resilience4jExecutorProperties executorProperties;
    private final Resilience4jExponentialBackoffProperties exponentialBackoffProperties;
    private final Resilience4jRetryProperties retryProperties;
    private final Resilience4jTimeLimiterProperties timeLimiterProperties;

    public ResilienceConfig(RestTemplateProperties restTemplateProperties, Resilience4jBulkheadProperties bulkheadProperties, Resilience4jCircuitBreakerProperties circuitBreakerProperties, Resilience4jExecutorProperties executorProperties, Resilience4jExponentialBackoffProperties exponentialBackoffProperties, Resilience4jRetryProperties retryProperties, Resilience4jTimeLimiterProperties timeLimiterProperties) {
        this.restTemplateProperties = restTemplateProperties;
        this.bulkheadProperties = bulkheadProperties;
        this.circuitBreakerProperties = circuitBreakerProperties;
        this.executorProperties = executorProperties;
        this.exponentialBackoffProperties = exponentialBackoffProperties;
        this.retryProperties = retryProperties;
        this.timeLimiterProperties = timeLimiterProperties;
    }

    /**
     * Configures a RestTemplate bean with custom connection and read timeouts.
     *
     * @return a configured RestTemplate instance.
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(restTemplateProperties.getConnectTimeout()); // 5 seconds
        requestFactory.setReadTimeout(restTemplateProperties.getReadTimeout()); // 10 seconds
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
                .maxConcurrentCalls(bulkheadProperties.getMaxConcurrentCalls()) // Allow up to 100 concurrent calls
                .maxWaitDuration(bulkheadProperties.getMaxWaitDuration()) // Wait up to 300 ms for a free slot
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

        // Handle sliding window size default logic here
        int slidingWindowSize = bulkheadProperties.getMaxConcurrentCalls(); // Default to maxConcurrentCalls if not set
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(circuitBreakerProperties.getFailureRateThreshold()) // The Circuit opens if 50% of calls fail.
                .waitDurationInOpenState(circuitBreakerProperties.getWaitDurationInOpenState()) // Wait 20 seconds before transitioning to half-open state.
                .permittedNumberOfCallsInHalfOpenState(circuitBreakerProperties.getPermittedNumberOfCallsInHalfOpenState()) // Allow 5 calls in half-open state.
                .slidingWindowSize(slidingWindowSize) // Use the calculated/retrieved size for metrics.
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
        RetryConfig.Builder<Object> builder = RetryConfig.custom()
                .maxAttempts(retryProperties.getMaxRetryAttempts()) // Retry up to 5 times
                .waitDuration(retryProperties.getRetryWaitDuration()) // Wait initially 200 ms between retries
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
                .retryOnResult(Objects::isNull); // Retry on a specific result
        // .retryOnPredicate(Predicate.not(response -> response.getStatusCode().is2xxSuccessful())) // Retry on non-2xx status codes

        // Conditionally add exponential backoff based on configuration if desired
        // Boolean flag in RetryDefaultProperties: useExponentialBackoff=true/false
        if (retryProperties.isUseExponentialBackoff()) {
            builder.intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                    exponentialBackoffProperties.getInitialIntervalMillis(),
                    exponentialBackoffProperties.getEbMultiplier(),
                    exponentialBackoffProperties.getRandomizationFactor(),
                    exponentialBackoffProperties.getMaxIntervalMillis()
            ));
        }

        return RetryRegistry.of(builder.build());
    }

    /**
     * Configures a TimeLimiterRegistry bean with custom timeout settings.
     *
     * @return a TimeLimiterRegistry instance.
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(timeLimiterProperties.getTimeoutDuration()) // Set timeout duration to 1 second
                .build();
        return TimeLimiterRegistry.of(config);
    }

    /**
     * Configures a ScheduledExecutorService bean for managing timeouts on captcha validation calls.
     *
     * @return a ScheduledExecutorService instance with a thread pool size of 4?.
     */
    @Bean(name = "captchaTimeLimiterExecutor") // Use the specific name required by the @Qualifier
    public ScheduledExecutorService captchaTimeLimiterExecutor() {
        // Choose an appropriate pool size.
        // Start with a small number and monitor.
        // This pool is specifically for managing timeouts on captcha validation calls.
        return Executors.newScheduledThreadPool(executorProperties.getCorePoolSize()); // Pool size
    }
}
