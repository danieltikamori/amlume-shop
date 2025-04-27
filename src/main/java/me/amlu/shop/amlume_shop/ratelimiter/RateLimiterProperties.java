/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

// package me.amlu.shop.amlume_shop.config.properties;
package me.amlu.shop.amlume_shop.ratelimiter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@ConfigurationProperties(prefix = "rate-limiter")
@Validated
public class RateLimiterProperties {

    /**
     * Global default settings for rate limiters.
     */
    @NotNull
    private LimiterConfig defaults = new LimiterConfig();

    /**
     * Specific configurations for named limiters. The key is the limiter name
     * (e.g., "captcha", "login", "asnLookup").
     */
    private Map<String, LimiterConfig> limiters = new HashMap<>();

    /**
     * Prefix for Redis keys used by the rate limiter.
     */
    @NotBlank
    private String redisKeyPrefix = "ratelimit:sw:"; // sw = sliding window

    /**
     * Whether to fail open (allow requests) if the rate limiter backend (Redis) fails.
     * Defaults to false (fail closed - deny requests).
     */
    private boolean failOpen = false;

    public RateLimiterProperties() {
    }

    public @NotNull LimiterConfig getDefaults() {
        return this.defaults;
    }

    public Map<String, LimiterConfig> getLimiters() {
        return this.limiters;
    }

    public @NotBlank String getRedisKeyPrefix() {
        return this.redisKeyPrefix;
    }

    public boolean isFailOpen() {
        return this.failOpen;
    }

    public void setDefaults(@NotNull LimiterConfig defaults) {
        this.defaults = defaults;
    }

    public void setLimiters(Map<String, LimiterConfig> limiters) {
        this.limiters = limiters;
    }

    public void setRedisKeyPrefix(@NotBlank String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }

    public void setFailOpen(boolean failOpen) {
        this.failOpen = failOpen;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof RateLimiterProperties other)) return false;
        if (!other.canEqual((Object) this)) return false;
        final Object this$defaults = this.getDefaults();
        final Object other$defaults = other.getDefaults();
        if (!Objects.equals(this$defaults, other$defaults)) return false;
        final Object this$limiters = this.getLimiters();
        final Object other$limiters = other.getLimiters();
        if (!Objects.equals(this$limiters, other$limiters)) return false;
        final Object this$redisKeyPrefix = this.getRedisKeyPrefix();
        final Object other$redisKeyPrefix = other.getRedisKeyPrefix();
        if (!Objects.equals(this$redisKeyPrefix, other$redisKeyPrefix))
            return false;
        return this.isFailOpen() == other.isFailOpen();
    }

    protected boolean canEqual(final Object other) {
        return other instanceof RateLimiterProperties;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $defaults = this.getDefaults();
        result = result * PRIME + ($defaults == null ? 43 : $defaults.hashCode());
        final Object $limiters = this.getLimiters();
        result = result * PRIME + ($limiters == null ? 43 : $limiters.hashCode());
        final Object $redisKeyPrefix = this.getRedisKeyPrefix();
        result = result * PRIME + ($redisKeyPrefix == null ? 43 : $redisKeyPrefix.hashCode());
        result = result * PRIME + (this.isFailOpen() ? 79 : 97);
        return result;
    }

    public String toString() {
        return "RateLimiterProperties(defaults=" + this.getDefaults() + ", limiters=" + this.getLimiters() + ", redisKeyPrefix=" + this.getRedisKeyPrefix() + ", failOpen=" + this.isFailOpen() + ")";
    }

    public static class LimiterConfig {
        /**
         * Maximum number of requests allowed within the window duration.
         */
        @Min(1)
        private long limit = 100;

        /**
         * The duration of the sliding window.
         */
        @NotNull
        private Duration windowDuration = Duration.ofMinutes(1);

        public LimiterConfig() {
        }

        public @Min(1) long getLimit() {
            return this.limit;
        }

        public @NotNull Duration getWindowDuration() {
            return this.windowDuration;
        }

        public void setLimit(@Min(1) long limit) {
            this.limit = limit;
        }

        public void setWindowDuration(@NotNull Duration windowDuration) {
            this.windowDuration = windowDuration;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof LimiterConfig other)) return false;
            if (!other.canEqual((Object) this)) return false;
            if (this.getLimit() != other.getLimit()) return false;
            final Object this$windowDuration = this.getWindowDuration();
            final Object other$windowDuration = other.getWindowDuration();
            return Objects.equals(this$windowDuration, other$windowDuration);
        }

        protected boolean canEqual(final Object other) {
            return other instanceof LimiterConfig;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $limit = this.getLimit();
            result = result * PRIME + Long.hashCode($limit);
            final Object $windowDuration = this.getWindowDuration();
            result = result * PRIME + ($windowDuration == null ? 43 : $windowDuration.hashCode());
            return result;
        }

        public String toString() {
            return "RateLimiterProperties.LimiterConfig(limit=" + this.getLimit() + ", windowDuration=" + this.getWindowDuration() + ")";
        }
    }

    // Method to get config for a specific limiter, falling back to defaults
    public LimiterConfig getConfigFor(String limiterName) {
        return limiters.getOrDefault(limiterName, defaults);
    }
}