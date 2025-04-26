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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Component
@ConfigurationProperties(prefix = "resilience4j.time-limiter.instances.default")
@Validated
public class Resilience4jTimeLimiterProperties {

    @NotNull
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration timeoutDuration = Duration.ofSeconds(1);

// --- Getters ---
    
    public Duration getTimeoutDuration() {
        return timeoutDuration;
    }

    // --- Setters ---

    public void setTimeoutDuration(Duration timeoutDuration) {
        this.timeoutDuration = timeoutDuration;
    }
}
