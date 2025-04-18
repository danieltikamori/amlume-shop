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
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "rate-limiter")
@Validated
@Data // Using Lombok @Data for boilerplate
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

    @Data
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
    }

    // Method to get config for a specific limiter, falling back to defaults
    public LimiterConfig getConfigFor(String limiterName) {
        return limiters.getOrDefault(limiterName, defaults);
    }
}