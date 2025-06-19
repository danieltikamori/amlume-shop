/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for monitoring circuit breakers.
 * <p>
 * This component reports the health status of all circuit breakers in the application.
 * It marks the health as DOWN if any circuit breaker is in OPEN or FORCED_OPEN state,
 * indicating that a service dependency is experiencing failures.
 * </p>
 * <p>
 * The health report includes detailed metrics for each circuit breaker:
 * <ul>
 *   <li>Current state (CLOSED, OPEN, HALF_OPEN, etc.)</li>
 *   <li>Failure rate percentage</li>
 *   <li>Slow call rate percentage</li>
 *   <li>Number of successful calls</li>
 *   <li>Number of failed calls</li>
 * </ul>
 * </p>
 */

@Component
public class ResilienceHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ResilienceHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean isDown = false;

        for (CircuitBreaker circuitBreaker : circuitBreakerRegistry.getAllCircuitBreakers()) {
            String name = circuitBreaker.getName();
            CircuitBreaker.State state = circuitBreaker.getState();

            Map<String, Object> circuitBreakerDetails = new HashMap<>();
            circuitBreakerDetails.put("state", state);
            circuitBreakerDetails.put("failureRate", circuitBreaker.getMetrics().getFailureRate() + "%");
            circuitBreakerDetails.put("slowCallRate", circuitBreaker.getMetrics().getSlowCallRate() + "%");
            circuitBreakerDetails.put("numberOfSuccessfulCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
            circuitBreakerDetails.put("numberOfFailedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());

            details.put(name, circuitBreakerDetails);

            if (state == CircuitBreaker.State.OPEN || state == CircuitBreaker.State.FORCED_OPEN) {
                isDown = true;
            }
        }

        if (isDown) {
            return Health.down().withDetails(details).build();
        } else {
            return Health.up().withDetails(details).build();
        }
    }
}
