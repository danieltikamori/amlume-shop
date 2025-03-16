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

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import me.amlu.shop.amlume_shop.security.service.CaptchaService;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MetricsConfig implements io.micrometer.prometheusmetrics.PrometheusConfig {
    @Bean
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry(PrometheusRegistry collectorRegistry) {
        return new PrometheusMeterRegistry(
                this,
                collectorRegistry,
                Clock.SYSTEM
        );
    }

    @Bean
    public PrometheusRegistry collectorRegistry(PrometheusMeterRegistry prometheusMeterRegistry) {
        return prometheusMeterRegistry.getPrometheusRegistry();
    }

    @Bean
    public MeterFilter meterFilter() {
        return MeterFilter.acceptNameStartsWith("paseto");
    }

    // originally collectorRegistry
    @Bean
    public CollectorRegistry prometheusClientCollectorRegistry() {
        return CollectorRegistry.defaultRegistry;
    }

    @Bean
    public MeterBinder captchaMetrics(CaptchaService captchaService) {
        return registry -> {
            // Register gauge for rate limit
            Gauge.builder("captcha.rate_limit.remaining",
                            () -> captchaService.getRemainingLimit("global"))
                    .description("Remaining rate limit capacity")
                    .register(registry);

            // Register counter for total validations
            Counter.builder("captcha.total")
                    .description("Total captcha validations")
                    .tag("type", "validation")
                    .register(registry);
        };
    }

    @Override
    public String get(@NotNull String key) {
        return null; // Use default values
    }
}
