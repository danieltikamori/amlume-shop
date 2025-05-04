/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.auth;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("authenticationHealth") // Give it a specific name if needed
public class AuthenticationHealthIndicator implements HealthIndicator {

    private final CircuitBreaker circuitBreaker;

    public AuthenticationHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry) {
        // Retrieve the specific circuit breaker by name from the registry
        // *** IMPORTANT: Replace "vaultService" with the actual name of the
        //     circuit breaker this indicator should monitor if it's different! ***
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("vaultService");
    }

    @Override
    public Health health() {
        CircuitBreaker.State state = circuitBreaker.getState();
        if (state == CircuitBreaker.State.CLOSED || state == CircuitBreaker.State.HALF_OPEN) {
            // Consider HALF_OPEN as UP or DEGRADED depending on the policy
            return Health.up()
                    .withDetail("circuitBreakerName", circuitBreaker.getName())
                    .withDetail("state", state)
                    .build();
        } else {
            return Health.down()
                    .withDetail("circuitBreakerName", circuitBreaker.getName())
                    .withDetail("state", state)
                    .withDetail("failureRate", circuitBreaker.getMetrics().getFailureRate())
                    .withDetail("slowCallRate", circuitBreaker.getMetrics().getSlowCallRate())
                    .build();
        }
    }
}

