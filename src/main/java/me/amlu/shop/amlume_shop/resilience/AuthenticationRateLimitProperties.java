/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.resilience;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "rate-limiter.limiters.authentication")
@Validated // Enable validation of constraints
public class AuthenticationRateLimitProperties {

    @Value("${rate-limiter.limiters.authentication.limit:100}")
    private int limit;

    @Value("${rate-limiter.limiters.authentication.window-duration:1h}")
    private Duration windowDuration;

    @Value("${rate-limiter.limiters.authentication.fail-open:false}")
    private boolean failOpen;

    // --- Getters ---
    public int getLimit() {
        return limit;
    }

    public Duration getWindowDuration() {
        return windowDuration;
    }

    public boolean isFailOpen() {
        return failOpen;
    }

    // --- Setters ---
    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void setWindowDuration(Duration windowDuration) {
        this.windowDuration = windowDuration;
    }

    public void setFailOpen(boolean failOpen) {
        this.failOpen = failOpen;
    }


//    // --- For TokenBucket implementation ---
//    /**
//     * Interval at which expired rate limiting state (e.g., keys in Redis) should be cleaned up.
//     */
//    @NotNull
//    private Duration cleanupInterval = Duration.ofHours(1);
//
//    /**
//     * The maximum number of tokens the bucket can hold (burst capacity).
//     */
//    @Min(1)
//    private long capacity = 100;
//
//    /**
//     * The interval at which tokens are added back to the bucket.
//     */
//    @NotNull
//    private Duration refillInterval = Duration.ofMinutes(1);
//
//    /**
//     * If true, allow requests when the rate limiter itself fails (e.g., Redis connection issue).
//     * If false, deny requests on rate limiter failure.
//     */
//    private boolean failOpen = false;
//
//    /**
//     * Whether login rate limiting is enabled.
//     */
//    private boolean enabled = true;
//
//    // --- Getters ---
//    public Duration getCleanupInterval() {
//        return cleanupInterval;
//    }
//
//    public long getCapacity() {
//        return capacity;
//    }
//
//    public Duration getRefillInterval() {
//        return refillInterval;
//    }
//
//    public boolean isFailOpen() {
//        return failOpen;
//    }
//
//    public boolean isEnabled() {
//        return enabled;
//    }
//
//    // --- Setters ---
//// Setters are needed for property binding unless using constructor binding
//    public void setCleanupInterval(Duration cleanupInterval) {
//        this.cleanupInterval = cleanupInterval;
//    }
//
//    public void setCapacity(long capacity) {
//        this.capacity = capacity;
//    }
//
//    public void setRefillInterval(Duration refillInterval) {
//        this.refillInterval = refillInterval;
//    }
//
//    public void setFailOpen(boolean failOpen) {
//        this.failOpen = failOpen;
//    }
//
//    public void setEnabled(boolean enabled) {
//        this.enabled = enabled;
//    }
}
