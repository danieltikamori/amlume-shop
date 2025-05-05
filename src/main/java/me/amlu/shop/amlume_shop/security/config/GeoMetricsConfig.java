/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeoMetricsConfig {
    /**
     * This configuration will provide metrics for:
     * 1. Memory usage (heap and non-heap)
     * 2. Garbage collection statistics
     * 3. Thread states
     * 4. File descriptor usage
     * <p>
     * Registers JVM metrics with the {@link MetricRegistry}:
     * <ul>
     *     <li>{@link MemoryUsageGaugeSet}</li>
     *     <li>{@link GarbageCollectorMetricSet}</li>
     *     <li>{@link ThreadStatesGaugeSet}</li>
     *     <li>{@link FileDescriptorRatioGauge}</li>
     * </ul>
     *
     * @return the {@link MetricRegistry} instance
     */
    @Bean
    public MetricRegistry metricRegistry() {
        MetricRegistry registry = new MetricRegistry();

        // Register JVM metrics
        registry.register("jvm.memory", new MemoryUsageGaugeSet());
        registry.register("jvm.gc", new GarbageCollectorMetricSet());
        registry.register("jvm.threads", new ThreadStatesGaugeSet());
        registry.register("jvm.files", new FileDescriptorRatioGauge());

        return registry;
    }
}
