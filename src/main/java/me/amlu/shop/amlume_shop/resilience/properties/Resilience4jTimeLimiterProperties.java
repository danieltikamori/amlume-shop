/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.resilience.properties;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Configuration properties for Resilience4j Time Limiter.
 *
 * @author Daniel Itiro Tikamori
 */
@ConfigurationProperties(prefix = "resilience4j.time-limiter.instances.default")
@Validated
public class Resilience4jTimeLimiterProperties {

    /**
     * The timeout duration for the time limiter.
     * <p>
     * This property is used to specify the maximum duration that a call can take before it is considered timed out.
     * The default value is 5 seconds.
     */
    @DurationMin(seconds = 0, message = "timeoutDuration must be greater than or equal to 0")
    @NotNull
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration timeoutDuration = Duration.ofSeconds(5);

// --- Getters ---

    public Duration getTimeoutDuration() {
        return timeoutDuration;
    }

    // --- Setters ---

    public void setTimeoutDuration(Duration timeoutDuration) {
        this.timeoutDuration = timeoutDuration;
    }
}
