/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service for making resilient calls to external APIs.
 * <p>
 * This service wraps external API calls with circuit breakers to prevent
 * cascading failures when external services are unavailable or slow.
 * It provides fallback mechanisms for graceful degradation.
 * </p>
 */
@Service
public class ExternalApiService {
    private static final Logger log = LoggerFactory.getLogger(ExternalApiService.class);

    private final RestTemplate restTemplate;
    private final CircuitBreakerFactory circuitBreakerFactory;

    /**
     * Creates a new ExternalApiService.
     *
     * @param restTemplate          The RestTemplate for making HTTP requests
     * @param circuitBreakerFactory The factory for creating circuit breakers
     */
    public ExternalApiService(RestTemplate restTemplate, CircuitBreakerFactory circuitBreakerFactory) {
        this.restTemplate = restTemplate;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    /**
     * Makes a resilient call to an external service with circuit breaker protection.
     * <p>
     * If the external service fails or times out, the call will be handled by
     * the fallback method.
     * </p>
     *
     * @param <T>          The expected response type
     * @param url          The URL of the external service
     * @param responseType The class representing the expected response type
     * @return The response from the external service, or a fallback value
     */
    public <T> T callExternalService(String url, Class<T> responseType) {
        return circuitBreakerFactory.create("externalService")
                .run(() -> restTemplate.getForObject(url, responseType),
                        throwable -> handleExternalServiceFailure(url, responseType, throwable));
    }

    /**
     * Handles failures when calling external services.
     * <p>
     * This method is called when the circuit breaker detects a failure.
     * It logs the error and returns a fallback value.
     * </p>
     *
     * @param <T>          The expected response type
     * @param url          The URL that failed
     * @param responseType The expected response type class
     * @param throwable    The exception that occurred
     * @return A fallback value (null by default)
     */
    private <T> T handleExternalServiceFailure(String url, Class<T> responseType, Throwable throwable) {
        log.error("External service call failed: {}", url, throwable);
        // Return fallback value or throw custom exception
        return null; // Replace with appropriate fallback
    }
}
