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

import java.time.temporal.ChronoUnit;
import java.time.Duration;

/**
 * Resilience4j Retry properties.
 * <p>
 * This class is used to bind the properties defined in the application.yml file under the
 * "resilience4j.retry.instances.default" prefix to a Java object.
 * </p>
 * <p>
 * The properties can be accessed using the getter methods provided by this class.
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "resilience4j.retry.instances.default")
@Validated
public class Resilience4jRetryProperties {

    /**
     * The number of retry attempts.
     * <p>
     * This property is used to specify the number of times a call should be retried
     * before giving up. The default value is 3.
     * </p>
     */
    @NotNull
    private int maxRetryAttempts = 3;

    /**
     * The wait duration between retries.
     * <p>
     * This property is used to specify the duration to wait between retry attempts.
     * The default value is 100 milliseconds.
     * </p>
     */
    @NotNull
    private long retryInterval = 1000L;

    /**
     * The wait duration between retries.
     * <p>
     * This property is used to specify the duration to wait between retry attempts.
     * The default value is 100 milliseconds.
     * </p>
     */
    @NotNull
    @DurationUnit(ChronoUnit.MILLIS) // Specify unit if property is just a number (e.g., 500)
    private Duration retryWaitDuration = Duration.ofMillis(500); // Default value

    private boolean useExponentialBackoff = false;

    // --- Getter ---

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public long getRetryInterval() {
        return retryInterval;
    }

    public Duration getRetryWaitDuration() {
        return retryWaitDuration;
    }

    // --- Setter ---

    public void setMaxRetryAttempts(Integer maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public void setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
    }

    public void setRetryWaitDuration(Duration retryWaitDuration) {
        this.retryWaitDuration = retryWaitDuration;
    }

    public boolean isUseExponentialBackoff() {
        return useExponentialBackoff;
    }
}
