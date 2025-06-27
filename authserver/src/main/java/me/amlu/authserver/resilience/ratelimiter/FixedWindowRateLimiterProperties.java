/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.resilience.ratelimiter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "security.fixed-window.rate-limit")
public class FixedWindowRateLimiterProperties {

    /**
     * Interval at which expired rate limiting state (e.g., keys in Redis) should be cleaned up.
     */
    @NotNull
    private Duration cleanupInterval = Duration.ofHours(1);

    /**
     * The maximum number of tokens the bucket can hold (burst capacity).
     */
    @Min(1)
    private long capacity = 100;

    /**
     * The interval at which tokens are added back to the bucket.
     */
    @NotNull
    private Duration refillInterval = Duration.ofMinutes(1);

    /**
     * If true, allow requests when the rate limiter itself fails (e.g., Redis connection issue).
     * If false, deny requests on rate limiter failure.
     */
    private boolean failOpen = false;

    /**
     * Whether login rate limiting is enabled.
     */
    private boolean enabled = true;

    private long ipLimit = 100;

    private long usernameLimit = 20;

    private long ipWindowSeconds = 60;

    private long usernameWindowSeconds = 60;


    // --- Getters ---
    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public long getCapacity() {
        return capacity;
    }

    public Duration getRefillInterval() {
        return refillInterval;
    }

    public boolean isFailOpen() {
        return failOpen;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // --- Setters ---
// Setters are needed for property binding unless using constructor binding
    public void setCleanupInterval(Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public void setRefillInterval(Duration refillInterval) {
        this.refillInterval = refillInterval;
    }

    public void setFailOpen(boolean failOpen) {
        this.failOpen = failOpen;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getIpLimit() {
        return ipLimit;
    }

    public void setIpLimit(long ipLimit) {
        this.ipLimit = ipLimit;
    }

    public long getUsernameLimit() {
        return usernameLimit;
    }

    public void setUsernameLimit(long usernameLimit) {
        this.usernameLimit = usernameLimit;
    }

    public long getDefaultLimit() {
        return 100;
    }

    public long getDefaultWindowSeconds() {
        return 60;
    }

    public long getIpWindowSeconds() {
        return ipWindowSeconds;
    }

    public void setIpWindowSeconds(long ipWindowSeconds) {
        this.ipWindowSeconds = ipWindowSeconds;
    }

    public long getUsernameWindowSeconds() {
        return usernameWindowSeconds;
    }

    public void setUsernameWindowSeconds(long usernameWindowSeconds) {
        this.usernameWindowSeconds = usernameWindowSeconds;
    }
}
