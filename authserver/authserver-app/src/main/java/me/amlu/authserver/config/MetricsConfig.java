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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import me.amlu.authserver.security.service.CaptchaService;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Objects;

/**
 * Configuration class for Micrometer metrics.
 * This class customizes the auto-configured MeterRegistry.
 */
@Configuration
public class MetricsConfig {

    // --- REMOVED prometheusMeterRegistry BEAN ---
    // Spring Boot's auto-configuration creates this bean for us.
    // Manually creating it is deprecated and unnecessary.

    // --- REMOVED prometheusConfig BEAN ---
    // This is also handled by auto-configuration.

    /**
     * Customizes the MeterRegistry by adding common tags to all metrics.
     * This helps in identifying metrics across different environments and applications.
     *
     * @param environment The Spring Environment to access application properties.
     * @return A {@link MeterRegistryCustomizer} that adds common tags.
     */
    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(Environment environment) {
        // Use Spring's Environment for a more robust way to get properties
        String appName = environment.getProperty("spring.application.name", "authserver");
        String envProfile = Objects.requireNonNullElse(environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : null, "development");

        return registry -> registry.config().commonTags(
                "application", appName,
                "environment", envProfile
        );
    }

    /**
     * Defines a {@link MeterFilter} to control which metrics are published.
     * This helps in reducing metric cardinality and focusing on important data.
     */
    @Bean
    public MeterFilter meterFilter() {
        return MeterFilter.denyUnless(meterId ->
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
}
