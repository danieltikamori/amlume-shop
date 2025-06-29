/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.resilience.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Configuration properties for Resilience4j Bulkhead.
 *
 * @author Daniel Itiro Tikamori
 */
@ConfigurationProperties(prefix = "resilience4j.bulkhead.instances.default")
@Validated // Optional: Enables JSR-303 validation
public class Resilience4jBulkheadProperties {

    /**
     * Maximum number of concurrent calls to the bulkhead.
     * Defaults to 25 if not specified in properties.
     */
    @Min(value = 1, message = "maxConcurrentCalls must be greater than or equal to 1")
    @NotNull
    private int maxConcurrentCalls = 25; // Default value

    /**
     * Maximum time duration the calling thread will wait to enter the bulkhead.
     * Defaults to 300ms if not specified in properties.
     */
    @DurationMin(millis = 0, message = "maxWaitDuration must be greater than or equal to 0")
    @NotNull
    @DurationUnit(ChronoUnit.MILLIS) // Specify unit if property is just a number (e.g., 500)
    private Duration maxWaitDuration = Duration.ofMillis(300); // Default value

    // --- Getter ---

    public int getMaxConcurrentCalls() {
        return maxConcurrentCalls;
    }

    public Duration getMaxWaitDuration() {
        return maxWaitDuration;
    }

    // --- Setter ---
    // Setter is required for Spring Boot to inject the value
    // Optional: You can add validation annotations to setters if needed

    public void setMaxConcurrentCalls(Integer maxConcurrentCalls) {
        this.maxConcurrentCalls = maxConcurrentCalls;
    }

    public void setMaxWaitDuration(Duration maxWaitDuration) {
        this.maxWaitDuration = maxWaitDuration;
    }
}
