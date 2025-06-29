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
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Resilience4j Exponential Backoff.
 * <p>
 * This class is used to configure the properties for the exponential backoff strategy in Resilience4j.
 *
 * @author Daniel Itiro Tikamori
 */
@ConfigurationProperties(prefix = "resilience4j.exponential-backoff.instances.default")
@Validated
@NullMarked
public class Resilience4jExponentialBackoffProperties {

    /**
     * The initial interval in milliseconds.
     * <p>
     * This property is used to specify the initial interval for the exponential backoff strategy.
     * The default value is 200 milliseconds.
     * </p>
     */
    @Min(value = 0, message = "initialIntervalMillis must be greater than or equal to 0")
    private long initialIntervalMillis = 200L; // Default value

    /**
     * The multiplier for the exponential backoff.
     * <p>
     * This property is used to specify the multiplier for the exponential backoff strategy.
     * The default value is 1.5.
     * </p>
     */
    @Min(value = 1, message = "ebMultiplier must be greater than or equal to 1")
    private double ebMultiplier = 1.5; // Default value

    /**
     * The randomization factor for the exponential backoff.
     * <p>
     * This property is used to specify the randomization factor for the exponential backoff strategy.
     * The default value is 0.5.
     * </p>
     */
    private double randomizationFactor = 0.5; // Default value

    /**
     * The maximum interval in milliseconds.
     * <p>
     * This property is used to specify the maximum interval for the exponential backoff strategy.
     * The default value is 86400000 milliseconds (24 hours).
     * </p>
     */
    @Min(value = 1000, message = "maxIntervalMillis must be greater than or equal to 1000")
    private long maxIntervalMillis = 86_400_000L; // Yes, we can use _ here, it's a long value

    // --- Getters ---

    public long getInitialIntervalMillis() {
        return initialIntervalMillis;
    }

    public double getEbMultiplier() {
        return ebMultiplier;
    }

    public double getRandomizationFactor() {
        return randomizationFactor;
    }

    public long getMaxIntervalMillis() {
        return maxIntervalMillis;
    }

    // --- Setters ---

    public void setInitialIntervalMillis(long initialIntervalMillis) {
        this.initialIntervalMillis = initialIntervalMillis;
    }

    public void setEbMultiplier(double ebMultiplier) {
        this.ebMultiplier = ebMultiplier;
    }

    public void setRandomizationFactor(double randomizationFactor) {
        this.randomizationFactor = randomizationFactor;
    }

    public void setMaxIntervalMillis(long maxIntervalMillis) {
        this.maxIntervalMillis = maxIntervalMillis;
    }
}
