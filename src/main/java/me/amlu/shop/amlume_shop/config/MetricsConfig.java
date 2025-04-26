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
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import me.amlu.shop.amlume_shop.security.service.CaptchaService;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@Configuration
public class MetricsConfig {
    // Removed implements PrometheusConfig - it's an interface, not meant to be implemented by config class directly

    // REMOVED: Redundant MeterRegistry bean - Let Actuator provide CompositeMeterRegistry
    // @Bean
    // MeterRegistry meterRegistry() {
    //     return new SimpleMeterRegistry();
    // }

    /**
     * Defines the Prometheus CollectorRegistry bean.
     * This bean should be simple and NOT depend on PrometheusMeterRegistry.
     */
    @Bean
    public CollectorRegistry collectorRegistry() {
        // Create a new, default CollectorRegistry.
        // It might be CollectorRegistry.defaultRegistry if you want the global one,
        // but a new instance is often preferred for application-specific metrics.
        return new CollectorRegistry(true); // 'true' enables default exports like JVM metrics
    }

    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry(PrometheusConfig prometheusConfig, PrometheusRegistry collectorRegistry, io.micrometer.core.instrument.Clock clock) { // Inject Clock
        // Ensure collectorRegistry is not null if injection somehow failed (optional check)
        if (collectorRegistry == null) {
            throw new IllegalStateException("CollectorRegistry bean is required for PrometheusMeterRegistry");
        }
        return new PrometheusMeterRegistry(prometheusConfig, collectorRegistry, clock);
    }


    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
                "application", "amlume-shop",
                "environment", "development" // Make environment configurable
        );
    }

    @Bean
    public PrometheusRegistry prometheusCollectorRegistry(PrometheusMeterRegistry prometheusMeterRegistry) { // Inject PrometheusMeterRegistry
        return prometheusMeterRegistry.getPrometheusRegistry();
    }

    /**
     * Provides the PrometheusConfig (usually the default is sufficient).
     */
    @Bean
    public PrometheusConfig prometheusConfig() {
        // You can customize PrometheusConfig here if needed
        // E.g., return key -> switch(key) { case "prometheus.descriptions": return "true"; default: return null; };
        return PrometheusConfig.DEFAULT;
    }

    @Bean
    public MeterFilter meterFilter() {
        // Example: Only accept metrics starting with specific prefixes
        return MeterFilter.denyUnless(meterId ->
                meterId.getName().startsWith("paseto") ||
                        meterId.getName().startsWith("captcha") ||
                        meterId.getName().startsWith("cache") ||
                        meterId.getName().startsWith("ratelimit") ||
                        meterId.getName().startsWith("valkey") ||
                        meterId.getName().startsWith("circuit_breaker") ||
                        // Allow common http/system metrics if desired
                        meterId.getName().startsWith("http.server.requests") ||
                        meterId.getName().startsWith("jvm.") ||
                        meterId.getName().startsWith("process.") ||
                        meterId.getName().startsWith("system.") ||
                        meterId.getName().startsWith("log4j2.") ||
                        meterId.getName().startsWith("tomcat.") ||
                        meterId.getName().startsWith("disk.")
        );
    }

    // REMOVED: Redundant CollectorRegistry bean - the primary one is defined above.
    // @Bean
    // public CollectorRegistry prometheusClientCollectorRegistry() {
    //     return CollectorRegistry.defaultRegistry; // This returns the static default registry, usually not needed as a separate bean
    // }

    @Bean
    public MeterBinder captchaMetrics(CaptchaService captchaService) {
        return registry -> {
            Gauge.builder("captcha.rate_limit.remaining",
                            () -> captchaService.getRemainingLimit("global"))
                    .description("Remaining captcha rate limit capacity (approximate)")
                    .register(registry);
            // Other counters are incremented directly in CaptchaService
        };
    }

    // --- Other metric beans (tokenValidationCounter, tokenValidationTimer, etc.) remain the same ---
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

    // Removed duplicate timer bean - tokenValidationTimer already covers this
    // @Bean
    // public Timer tokenValidationLatency(MeterRegistry meterRegistry) { ... }

    // Removed implements PrometheusConfig and the get() method override
    // @Override
    // public String get(@NotNull String key) {
    //     return null;
    // }

    @Bean
    public Timer tokenClaimsValidationLatency(MeterRegistry meterRegistry) {
        return Timer.builder("paseto.token.claims.validation")
                .description("Time spent validating PASETO token claims")
                .tags("type", "claims_validation")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(2))
                .register(meterRegistry);
    }
}

