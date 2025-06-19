/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Resilience4j circuit breakers and time limiters.
 * <p>
 * This class provides customized circuit breaker configurations for different
 * service types with appropriate thresholds and timeouts to improve system
 * resilience during failures.
 * </p>
 *
 * @see CircuitBreakerConfig
 * @see TimeLimiterConfig
 */

@Configuration
public class ResilienceConfig {

    /**
     * Provides default circuit breaker configuration for internal services.
     * <p>
     * Configuration parameters:
     * <ul>
     *   <li>Sliding window size: 10 requests</li>
     *   <li>Failure rate threshold: 50%</li>
     *   <li>Wait duration in open state: 10 seconds</li>
     *   <li>Permitted calls in half-open state: 5</li>
     *   <li>Slow call rate threshold: 50%</li>
     *   <li>Slow call duration threshold: 2 seconds</li>
     *   <li>Timeout duration: 3 seconds</li>
     * </ul>
     * </p>
     *
     * @return A customizer for the default circuit breaker configuration
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(5)
                        .slowCallRateThreshold(50)
                        .slowCallDurationThreshold(Duration.ofSeconds(2))
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(3))
                        .build())
                .build());
    }

    /**
     * Provides circuit breaker configuration specifically for external services.
     * <p>
     * External services may have different reliability characteristics,
     * so this configuration is more conservative with:
     * <ul>
     *   <li>Larger sliding window size: 20 requests</li>
     *   <li>Lower failure rate threshold: 40%</li>
     *   <li>Longer wait duration in open state: 30 seconds</li>
     *   <li>More permitted calls in half-open state: 10</li>
     *   <li>Longer timeout duration: 5 seconds</li>
     * </ul>
     * </p>
     *
     * @return A customizer for the external service circuit breaker configuration
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> externalServiceCustomizer() {
        return factory -> factory.configure(builder -> builder
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(20)
                        .failureRateThreshold(40)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .permittedNumberOfCallsInHalfOpenState(10)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build()), "externalService");
    }
}
