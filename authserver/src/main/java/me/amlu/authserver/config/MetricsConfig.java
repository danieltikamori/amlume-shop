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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import me.amlu.authserver.security.service.CaptchaService;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.config.MeterFilter;

import java.time.Duration;

/**
 * Configuration class for Micrometer metrics.
 * This class defines various beans for Prometheus integration, common tags,
 * and specific metrics related to the application's functionality like PASETO token validation
 * and CAPTCHA rate limiting.
 */
@Configuration
public class MetricsConfig {

    /**
     * Configures and provides a {@link PrometheusMeterRegistry} bean.
     * This registry is the core component for collecting and exposing metrics in a Prometheus-compatible format.
     *
     * @param prometheusConfig            The Prometheus configuration.
     * @param prometheusCollectorRegistry The Prometheus collector registry from the Prometheus client library.
     * @param clock                       The clock used by Micrometer for time-related operations.
     * @return A configured {@link PrometheusMeterRegistry}.
     */
    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry(PrometheusConfig prometheusConfig, io.prometheus.metrics.model.registry.PrometheusRegistry prometheusCollectorRegistry, io.micrometer.core.instrument.Clock clock) {
        return new PrometheusMeterRegistry(prometheusConfig, prometheusCollectorRegistry, clock);
    }

    /**
     * Customizes the MeterRegistry by adding common tags to all metrics.
     * This helps in identifying metrics across different environments and applications.
     *
     * @return A {@link MeterRegistryCustomizer} that adds common tags.
     */
    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        String environment = System.getProperty("app.environment", "development");
        return registry -> registry.config().commonTags(
                "application", "amlume-shop",
                "environment", environment
        );
    }

    @Bean
    /*
     * Provides the default Prometheus configuration.
     *
     * @return The default {@link PrometheusConfig}.
     */
    public PrometheusConfig prometheusConfig() {
        return PrometheusConfig.DEFAULT;
    }

    /**
     * Defines a {@link MeterFilter} to control which metrics are published.
     * This filter denies all metrics unless their names start with specific prefixes,
     * effectively allowing only relevant application and system metrics to be exposed.
     * This helps in reducing metric cardinality and focusing on important data.
     *
     * <p><b>Usage:</b> Metrics whose names start with any of the specified prefixes will be allowed.
     * All other metrics will be denied.</p>
     */
    @Bean
    public MeterFilter meterFilter() {
        return MeterFilter.denyUnless(meterId ->
                meterId.getName().startsWith("paseto") ||
                        meterId.getName().startsWith("captcha") ||
                        meterId.getName().startsWith("cache") ||
                        meterId.getName().startsWith("ratelimit") ||
                        meterId.getName().startsWith("valkey") ||
                        meterId.getName().startsWith("circuit_breaker") ||
                        meterId.getName().startsWith("http.server.requests") ||
                        meterId.getName().startsWith("jvm.") ||
                        meterId.getName().startsWith("process.") ||
                        meterId.getName().startsWith("system.") ||
                        meterId.getName().startsWith("log4j2.") ||
                        meterId.getName().startsWith("tomcat.") ||
                        meterId.getName().startsWith("disk.")
        );
    }

    /**
     * Binds CAPTCHA related metrics to the MeterRegistry.
     * Specifically, it registers a Gauge to track the remaining CAPTCHA rate limit capacity.
     *
     * @param captchaService The {@link CaptchaService} to retrieve CAPTCHA related data.
     * @return A {@link MeterBinder} that registers CAPTCHA metrics.
     */
    @Bean
    public MeterBinder captchaMetrics(CaptchaService captchaService) {
        return registry -> {
            Gauge.builder("captcha.rate_limit.remaining",
                            () -> captchaService.getRemainingLimit("global"))
                    .description("Remaining captcha rate limit capacity (approximate)")
                    .register(registry);
        };
    }

    /**
     * Defines a Counter bean named 'tokenValidationCounter' to track the total number of PASETO token validations.
     * This counter increments every time a PASETO token validation attempt occurs.
     *
     * @param meterRegistry The {@link MeterRegistry} to register the counter with.
     * @return The configured {@link Counter} bean.
     */
    @Bean
    public Counter tokenValidationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("paseto_token_validation_total")
                .description("Total number of PASETO token validations")
                .register(meterRegistry);
    }

    /**
     * Defines a Timer bean named 'tokenValidationTimer' to measure the latency
     * of the overall PASETO token validation process.
     *
     * @param meterRegistry The MeterRegistry to register the timer with.
     * @return The configured Timer bean.
     */
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

    /**
     * Defines a Timer bean named 'tokenClaimsValidationLatency' to measure the latency
     * specifically for validating the claims within a PASETO token.
     *
     * @param meterRegistry The MeterRegistry to register the timer with.
     * @return The configured Timer bean.
     */
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
