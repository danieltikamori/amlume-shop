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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import me.amlu.shop.amlume_shop.security.service.CaptchaService;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@Configuration
public class MetricsConfig implements io.micrometer.prometheusmetrics.PrometheusConfig {

    @Bean
    MeterRegistry meterRegistry() {
        // Consider using CompositeMeterRegistry if multiple registries are needed
        // Or rely on Spring Boot Actuator's auto-configuration
        return new SimpleMeterRegistry();
    }

    // Provide a Clock bean for dependency injection
    // @Bean
    // public Clock clock() {
    //     return Clock.systemUTC();
    // }


    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry(PrometheusRegistry collectorRegistry, io.micrometer.core.instrument.Clock clock) { // Inject Clock
        PrometheusMeterRegistry prometheusMeterRegistry = new PrometheusMeterRegistry(
                this,
                collectorRegistry,
                clock
        );
        return prometheusMeterRegistry;
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
                "application", "amlume-shop",
                "environment", "development" // Make environment configurable
        );
    }

    @Bean
    public PrometheusRegistry collectorRegistry(PrometheusMeterRegistry prometheusMeterRegistry) { // Inject PrometheusMeterRegistry
        return prometheusMeterRegistry.getPrometheusRegistry();
    }

    @Bean
    public MeterFilter meterFilter() {
        // Example: Only accept metrics starting with "paseto" or "captcha"
        return MeterFilter.denyUnless(meterId ->
                meterId.getName().startsWith("paseto") ||
                        meterId.getName().startsWith("captcha") ||
                        meterId.getName().startsWith("cache") || meterId.getName().startsWith("ratelimit") || meterId.getName().startsWith("valkey") || meterId.getName().startsWith("circuit_breaker")
        );
        // return MeterFilter.denyNameStartsWith("jvm"); // Example: Deny JVM metrics
    }

    @Bean
    public CollectorRegistry prometheusClientCollectorRegistry() {
        return CollectorRegistry.defaultRegistry;
    }

    @Bean
    public MeterBinder captchaMetrics(CaptchaService captchaService) {
        return registry -> {
            // Register gauge for rate limit remaining
            // Note: This now relies on CaptchaService.getRemainingLimit which uses ResilienceService
            Gauge.builder("captcha.rate_limit.remaining",
                            // Use a lambda to call the service method, handle potential errors if needed
                            () -> captchaService.getRemainingLimit("global")) // Assuming "global" or pass relevant key if needed
                    .description("Remaining captcha rate limit capacity (approximate)")
                    .register(registry);

            // Register counter for total validations (already present in CaptchaService)
            // Counter.builder("captcha.total")
            //        .description("Total captcha validations")
            //        .tag("type", "validation")
            //        .register(registry);

            // Other counters (like success/error/ratelimit exceeded) are incremented directly in CaptchaService
        };
    }

    // --- Other metric beans remain the same ---
    @Bean
    public Counter tokenValidationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("paseto_token_validation_total")
                .description("Total number of PASETO token validations")
                .register(meterRegistry);
    }

    @Bean
    public Timer tokenValidationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("paseto.token.validation")
                .description("Time spent validating PASETO tokens")
                .tags("type", "validation")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(10))
                .register(meterRegistry);
    }

    @Bean
    public Timer tokenValidationLatency(MeterRegistry meterRegistry) {
        return Timer.builder("paseto_token_validation_seconds")
                .description("Time spent validating PASETO tokens")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .distributionStatisticExpiry(Duration.ofMinutes(5))
                .distributionStatisticBufferLength(5)
                .register(meterRegistry);
    }
    @Override
    public String get(@NotNull String key) {
        // This method is part of the PrometheusConfig interface,
        // return null or handle specific Prometheus config keys if needed
        return null;
    }

    // Timer bean for claims validation latency
    @Bean
    public Timer tokenClaimsValidationLatency(MeterRegistry meterRegistry) {
        return Timer.builder("paseto.token.claims.validation")
                .description("Time spent validating PASETO token claims")
                .tags("type", "claims_validation")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(2)) // Claims validation should be faster
                .register(meterRegistry);
    }
}
