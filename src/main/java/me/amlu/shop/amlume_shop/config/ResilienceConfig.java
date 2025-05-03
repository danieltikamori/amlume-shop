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
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import me.amlu.shop.amlume_shop.resilience.properties.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
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
        Resilience4jExponentialBackoffProperties.class, // Keep this if used for exponential backoff params
        Resilience4jRetryProperties.class,
        Resilience4jTimeLimiterProperties.class,
        RestTemplateProperties.class
})
public class ResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class); // Add logger

    private final RestTemplateProperties restTemplateProperties;
    private final Resilience4jBulkheadProperties bulkheadProperties;
    private final Resilience4jCircuitBreakerProperties circuitBreakerProperties;
    private final Resilience4jExecutorProperties executorProperties;
    private final Resilience4jExponentialBackoffProperties exponentialBackoffProperties; // Keep if used
    private final Resilience4jRetryProperties retryProperties; // Keep this injection
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
     * Configures a RetryRegistry bean by creating Retry instances for each configuration
     * defined under `resilience4j.retry.instances` in the properties.
     *
     * @return a RetryRegistry instance populated with configured Retry instances.
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryRegistry registry = RetryRegistry.ofDefaults(); // Start with an empty/default registry

        log.info("Configuring Resilience4j Retry instances...");
        // Iterate through the configured instances from properties
        for (Map.Entry<String, Resilience4jRetryProperties.RetryConfig> entry : retryProperties.getInstances().entrySet()) {
            String instanceName = entry.getKey();
            Resilience4jRetryProperties.RetryConfig instanceConfig = entry.getValue(); // Get the nested config

            log.debug("Building Retry config for instance: {}", instanceName);

            // --- Build RetryConfig using values from instanceConfig ---
            RetryConfig.Builder<Object> builder = RetryConfig.custom();

            // Max Attempts (Error was getMaxRetryAttempts)
            builder.maxAttempts(instanceConfig.getMaxAttempts()); // Use getter from nested config

            // Wait Duration (Error was getRetryWaitDuration/getRetryInterval)
            Duration waitDuration = instanceConfig.getWaitDuration(); // Use getter from nested config

            // Interval Function (Error was isUseExponentialBackoff)
            IntervalFunction intervalFunction;
            // Use the enable flag from the specific instance config
            if (instanceConfig.isEnableExponentialBackoff()) {
                log.debug("Exponential backoff enabled for instance: {}", instanceName);
                // Use global exponential backoff properties if enabled for this instance
                intervalFunction = IntervalFunction.ofExponentialRandomBackoff(
                        exponentialBackoffProperties.getInitialIntervalMillis(), // Use global initial interval
                        exponentialBackoffProperties.getEbMultiplier(), // Use global multiplier
                        exponentialBackoffProperties.getRandomizationFactor(), // Use global randomization
                        exponentialBackoffProperties.getMaxIntervalMillis() // Use global max interval
                );
                // Note: instanceConfig.getWaitDuration() is effectively ignored when exponential backoff is enabled here.
            } else {
                log.debug("Fixed interval wait enabled for instance: {}", instanceName);
                intervalFunction = IntervalFunction.of(waitDuration); // Use instance-specific fixed duration
            }
            builder.intervalFunction(intervalFunction);

            // Retry Exceptions
            if (instanceConfig.getRetryExceptions() != null && !instanceConfig.getRetryExceptions().isEmpty()) {
                builder.retryExceptions(instanceConfig.getRetryExceptions().toArray(new Class[0])); // Use retryOnExceptions
                log.debug("Configured retryExceptions for {}: {}", instanceName, instanceConfig.getRetryExceptions());
            }

            // Ignore Exceptions
            if (instanceConfig.getIgnoreExceptions() != null && !instanceConfig.getIgnoreExceptions().isEmpty()) {
                builder.ignoreExceptions(instanceConfig.getIgnoreExceptions().toArray(new Class[0]));
                log.debug("Configured ignoreExceptions for {}: {}", instanceName, instanceConfig.getIgnoreExceptions());
            }

            // --- Deprecated/Removed Logic from original code ---
            // The original code had complex retryOnException/retryOnResult logic here.
            // This logic should ideally be defined per-instance in the YAML/RetryConfig if needed,
            // or applied when decorating specific methods, not globally in the registry setup.
            // builder.retryOnException(t -> t instanceof RestClientException || (t instanceof SocketTimeoutException));
            // builder.retryOnResult(Objects::isNull);
            // --- End Deprecated/Removed Logic ---

            // Build the final RetryConfig for this instance
            RetryConfig finalRetryConfig = builder.build();

            // Create and register the Retry instance
            Retry retry = registry.retry(instanceName, finalRetryConfig);
            log.info("Successfully configured and registered Resilience4j Retry instance: {}", instanceName);

            // Optional: Add event listeners here if needed globally for this instance
            // retry.getEventPublisher().onRetry(event -> log.info("Retrying operation {} attempt #{}", instanceName, event.getNumberOfRetryAttempts()));
        }

        return registry;
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
     * @return a ScheduledExecutorService instance with a thread pool size based on properties.
     */
    @Bean(name = "captchaTimeLimiterExecutor") // Use the specific name required by the @Qualifier
    public ScheduledExecutorService captchaTimeLimiterExecutor() {
        // Choose an appropriate pool size based on properties.
        log.info("Creating ScheduledExecutorService 'captchaTimeLimiterExecutor' with core pool size: {}", executorProperties.getCorePoolSize());
        return Executors.newScheduledThreadPool(executorProperties.getCorePoolSize()); // Pool size from properties
    }
}