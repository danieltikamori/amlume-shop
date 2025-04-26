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

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Configuration properties for Resilience4j Circuit Breaker.
 *
 * @author Daniel Itiro Tikamori
 */
@ConfigurationProperties(prefix = "resilience4j.circuitbreaker.instances.default")
@Validated
public class Resilience4jCircuitBreakerProperties {

    @Min(value = 0, message = "failureRateThreshold must be greater than or equal to 0")
    @NotNull
    private float failureRateThreshold = 50.0f;

    @Min(value = 0, message = "waitDurationInOpenState must be greater than or equal to 0")
    @NotNull
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration waitDurationInOpenState = Duration.ofSeconds(20);

    @Min(value = 1, message = "permittedNumberOfCallsInHalfOpenState must be greater than or equal to 1")
    @NotNull
    private int permittedNumberOfCallsInHalfOpenState = 3;

    @NotNull
    private int slidingWindowSize; // No default here, will handle in main config

    // --- Getter ---

    public float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public @NotNull Duration getWaitDurationInOpenState() {
        return this.waitDurationInOpenState;
    }

    public @NotNull int getPermittedNumberOfCallsInHalfOpenState() {
        return this.permittedNumberOfCallsInHalfOpenState;
    }

    public @NotNull int getSlidingWindowSize() {
        return this.slidingWindowSize;
    }

    // --- Setter ---

    public void setFailureRateThreshold(@NotNull float failureRateThreshold) {
        this.failureRateThreshold = failureRateThreshold;
    }

    public void setWaitDurationInOpenState(@NotNull Duration waitDurationInOpenState) {
        this.waitDurationInOpenState = waitDurationInOpenState;
    }

    public void setPermittedNumberOfCallsInHalfOpenState(@NotNull int permittedNumberOfCallsInHalfOpenState) {
        this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
    }

    public void setSlidingWindowSize(@NotNull int slidingWindowSize) {
        this.slidingWindowSize = slidingWindowSize;
    }

//    public void setMaxConcurrentCalls(int maxConcurrentCalls) {
//        this.maxConcurrentCalls = maxConcurrentCalls;
//    }
}
