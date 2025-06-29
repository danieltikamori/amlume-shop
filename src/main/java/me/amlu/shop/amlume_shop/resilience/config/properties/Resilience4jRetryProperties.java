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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resilience4j Retry properties.
 * <p>
 * This class is used to bind the properties defined in the application.yml file under the
 * "resilience4j.retry" prefix to a Java object, including configurations for multiple instances.
 * </p>
 *
 * @author Daniel Itiro Tikamori
 */
@Component("retryProperties") // Register as a bean with the specific name "retryProperties"
@ConfigurationProperties(prefix = "resilience4j.retry") // Bind properties from "resilience4j.retry"
@Validated // Enable validation for this class and nested RetryConfig
public class Resilience4jRetryProperties {

    // Map to hold configurations for named instances (e.g., asnLookup, vaultService)
    @Valid // Ensure nested RetryConfig objects are also validated
    private Map<String, RetryConfig> instances = new HashMap<>();

    // --- Getters and Setters ---

    public Map<String, RetryConfig> getInstances() {
        return instances;
    }

    public void setInstances(Map<String, RetryConfig> instances) {
        this.instances = instances;
    }

    // --- equals, hashCode, toString ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resilience4jRetryProperties that = (Resilience4jRetryProperties) o;
        return Objects.equals(instances, that.instances);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instances);
    }

    @Override
    public String toString() {
        return "Resilience4jRetryProperties{" +
                "instances=" + instances +
                '}';
    }

    // --- Nested Static Class for Instance Configuration ---
    // This holds the properties defined *within* each instance in the YAML
    @Validated // Enable validation for fields within RetryConfig
    public static class RetryConfig {

        /**
         * The maximum number of retry attempts (including the initial call).
         */
        @Min(value = 1, message = "maxAttempts must be greater than or equal to 1")
        @NotNull(message = "maxAttempts cannot be null")
        private Integer maxAttempts = 3; // Default from original class

        /**
         * The base wait duration between retries.
         */
        @DurationMin(millis = 0, message = "waitDuration must be non-negative")
        @NotNull(message = "waitDuration cannot be null")
        @DurationUnit(ChronoUnit.MILLIS) // Specify unit if property is just a number (e.g., 500)
        private Duration waitDuration = Duration.ofMillis(500); // Default from original class (retryWaitDuration)

        /**
         * Whether to enable exponential backoff.
         */
        private boolean enableExponentialBackoff = false; // Default from original class

        /**
         * Multiplier for exponential backoff. Only used if enableExponentialBackoff is true.
         */
        private Double exponentialBackoffMultiplier; // Optional, only relevant if enabled

        /**
         * List of exceptions that should trigger a retry.
         */
        private List<Class<? extends Throwable>> retryExceptions = List.of(); // Default empty

        /**
         * List of exceptions that should NOT trigger a retry (they cause immediate failure).
         */
        private List<Class<? extends Throwable>> ignoreExceptions = List.of(); // Default empty


        // --- Getters and Setters for RetryConfig ---

        public Integer getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getWaitDuration() {
            return waitDuration;
        }

        public void setWaitDuration(Duration waitDuration) {
            this.waitDuration = waitDuration;
        }

        public boolean isEnableExponentialBackoff() {
            return enableExponentialBackoff;
        }

        public void setEnableExponentialBackoff(boolean enableExponentialBackoff) {
            this.enableExponentialBackoff = enableExponentialBackoff;
        }

        public Double getExponentialBackoffMultiplier() {
            return exponentialBackoffMultiplier;
        }

        public void setExponentialBackoffMultiplier(Double exponentialBackoffMultiplier) {
            this.exponentialBackoffMultiplier = exponentialBackoffMultiplier;
        }

        public List<Class<? extends Throwable>> getRetryExceptions() {
            return retryExceptions;
        }

        public void setRetryExceptions(List<Class<? extends Throwable>> retryExceptions) {
            this.retryExceptions = retryExceptions;
        }

        public List<Class<? extends Throwable>> getIgnoreExceptions() {
            return ignoreExceptions;
        }

        public void setIgnoreExceptions(List<Class<? extends Throwable>> ignoreExceptions) {
            this.ignoreExceptions = ignoreExceptions;
        }

        // --- equals, hashCode, toString for RetryConfig ---
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RetryConfig that = (RetryConfig) o;
            return enableExponentialBackoff == that.enableExponentialBackoff && Objects.equals(maxAttempts, that.maxAttempts) && Objects.equals(waitDuration, that.waitDuration) && Objects.equals(exponentialBackoffMultiplier, that.exponentialBackoffMultiplier) && Objects.equals(retryExceptions, that.retryExceptions) && Objects.equals(ignoreExceptions, that.ignoreExceptions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(maxAttempts, waitDuration, enableExponentialBackoff, exponentialBackoffMultiplier, retryExceptions, ignoreExceptions);
        }

        @Override
        public String toString() {
            return "RetryConfig{" +
                    "maxAttempts=" + maxAttempts +
                    ", waitDuration=" + waitDuration +
                    ", enableExponentialBackoff=" + enableExponentialBackoff +
                    ", exponentialBackoffMultiplier=" + exponentialBackoffMultiplier +
                    ", retryExceptions=" + retryExceptions +
                    ", ignoreExceptions=" + ignoreExceptions +
                    '}';
        }
    }
}
