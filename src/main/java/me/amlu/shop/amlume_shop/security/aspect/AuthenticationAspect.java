/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.aspect;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry; // Import registry
import io.github.resilience4j.retry.RetryRegistry;
import io.lettuce.core.RedisConnectionException;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
// Removed unused import: import me.amlu.shop.amlume_shop.resilience.config.ResilienceConfig;
import me.amlu.shop.amlume_shop.security.config.properties.AuthenticationAspectProperties; // Import the correct properties class
import me.amlu.shop.amlume_shop.exceptions.CircuitBreakerOpenException;
import me.amlu.shop.amlume_shop.exceptions.TokenValidationFailureException;
import me.amlu.shop.amlume_shop.exceptions.UnauthorizedException;
// Removed unused import: import me.amlu.shop.amlume_shop.resilience.properties.Resilience4jRetryProperties;
import me.amlu.shop.amlume_shop.security.paseto.TokenValidationService; // Assuming this validates your Bearer token
import me.amlu.shop.amlume_shop.user_management.UserService; // Use interface
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier; // Import Qualifier
import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.retry.RetryPolicy;
// Removed unused import: import org.springframework.retry.backoff.ExponentialBackOffPolicy;
// Removed unused import: import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.BinaryExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.security.SignatureException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static me.amlu.shop.amlume_shop.commons.Constants.*;

/**
 * Aspect responsible for handling authentication based on the @RequiresAuthentication annotation.
 * It extracts a Bearer token, validates it using TokenValidationService,
 * fetches the user, sets the SecurityContext, and applies resilience patterns.
 *
 * @see RequiresAuthentication
 * @see TokenValidationService
 */

/**
 * Aspect responsible for handling authentication based on the @RequiresAuthentication annotation.
 * It extracts a Bearer token, validates it using TokenValidationService,
 * fetches the user, sets the SecurityContext, and applies resilience patterns (Circuit Breaker and Retry).
 * <p>
 * This aspect is ordered first to ensure authentication is performed before other security checks like role authorization.
 *
 * @see RequiresAuthentication
 * @see TokenValidationService
 */
@Aspect
@Component
@Order(1) // Ensure authentication check runs before other aspects like role checks
public class AuthenticationAspect {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationAspect.class);

    // These fields are for the internal Spring RetryTemplate
    private final long retryInterval; // Changed to long to match FixedBackOffPolicy setter
    private final int maxRetryAttempts;

    private final UserService userService; // Use interface
    private final TokenValidationService tokenValidationService; // Service to validate the token
    private final MeterRegistry meterRegistry;
    private final CircuitBreaker circuitBreaker; // Resilience4j Circuit Breaker
    private final RetryTemplate retryTemplate; // Spring Retry Template

    // Constructor updated to inject AuthenticationAspectProperties
    public AuthenticationAspect(
            @NonNull UserService userService,
            @NonNull TokenValidationService tokenValidationService,
            @NonNull MeterRegistry meterRegistry,
            @Qualifier("circuitBreakerRegistry") CircuitBreakerRegistry circuitBreakerRegistry, // Inject Resilience4j registry
            @Qualifier("retryRegistry") RetryRegistry resilience4jRetryRegistry, // Inject Resilience4j registry (keep if used elsewhere or intended)
            // Inject the specific properties class for this aspect's retry config
            AuthenticationAspectProperties authAspectProperties
    ) {
        this.userService = userService;
        this.tokenValidationService = tokenValidationService;
        this.meterRegistry = meterRegistry;

        // --- Initialize fields using the correct properties bean ---
        // Convert Duration from properties to long millis for FixedBackOffPolicy
        this.retryInterval = authAspectProperties.getRetryInterval().toMillis();
        this.maxRetryAttempts = authAspectProperties.getMaxRetryAttempts();

        // --- Resilience4j Circuit Breaker Setup ---
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("authenticationAspectBreaker", createCircuitBreakerConfig());
        configureCircuitBreakerMetrics(this.circuitBreaker);

        // --- Spring Retry Template Setup (using values from authAspectProperties) ---
        // Pass the Resilience4j retryRegistry if you intend to integrate listeners later, otherwise it's unused here
        this.retryTemplate = createRetryTemplate(resilience4jRetryRegistry);
    }

    // --- Resilience Configuration ---

    // Resilience4j Circuit Breaker Config
    private CircuitBreakerConfig createCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(10)
                .slidingWindowSize(100)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(10)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions( // Exceptions that should count towards failure rate
                        IOException.class,
                        TimeoutException.class,
                        RedisConnectionException.class, // If Redis is still used elsewhere
                        TokenValidationFailureException.class, // If token validation service throws this
                        SignatureException.class // If token validation service throws this
                )
                .ignoreExceptions( // Exceptions that should NOT count (e.g., client errors)
                        IllegalArgumentException.class,
                        UnauthorizedException.class // Don't trip breaker for auth failures
                )
                .build();
    }

    // Spring Retry Template Factory (Uses fields initialized from AuthenticationAspectProperties)
    private RetryTemplate createRetryTemplate(RetryRegistry resilience4jRetryRegistry) { // Parameter kept for potential future use
        RetryTemplate localRetryTemplate = new RetryTemplate();

        CompositeRetryPolicy retryPolicy = getCompositeRetryPolicy(this.maxRetryAttempts); // Use field

        // For user authentication, usually the fixed policy is more adequate.
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(this.retryInterval); // Use field (already in millis)

        localRetryTemplate.setRetryPolicy(retryPolicy);
        localRetryTemplate.setBackOffPolicy(backOffPolicy);

        // Optional: Add listeners (potentially bridging from Resilience4j registry if needed)
        // resilience4jRetryRegistry.retry("someInstanceName").getEventPublisher()...
        // localRetryTemplate.registerListener(...);

        return localRetryTemplate;
    }

    // Spring Retry Policy Factory (Remains the same)
    @NotNull
    private static CompositeRetryPolicy getCompositeRetryPolicy(int maxRetryAttempts) {
        BinaryExceptionClassifier defaultClassifier = new BinaryExceptionClassifier(Map.of(
                IOException.class, true,
                TimeoutException.class, true,
                RedisConnectionException.class, true // If applicable.
                // Add other transient exceptions if needed
        ));

        CompositeRetryPolicy retryPolicy = new CompositeRetryPolicy();
        BinaryExceptionClassifierRetryPolicy binaryExceptionRetryPolicy = new BinaryExceptionClassifierRetryPolicy(defaultClassifier);
        MaxAttemptsRetryPolicy maxAttemptsRetryPolicy = new MaxAttemptsRetryPolicy(maxRetryAttempts); // Use parameter

        retryPolicy.setPolicies(new RetryPolicy[]{binaryExceptionRetryPolicy, maxAttemptsRetryPolicy});
        return retryPolicy;
    }

    // --- Metrics Configuration (Remains the same) ---

    private void configureCircuitBreakerMetrics(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
                .onSuccess(event -> meterRegistry.counter("circuit_breaker.calls", "name", circuitBreaker.getName(), "type", "successful").increment())
                .onError(event -> meterRegistry.counter("circuit_breaker.calls", "name", circuitBreaker.getName(), "type", "failed").increment())
                .onStateTransition(event -> {
                    log.info("Circuit breaker '{}' state changed from {} to {}", circuitBreaker.getName(), event.getStateTransition().getFromState(), event.getStateTransition().getToState());
                    meterRegistry.counter("circuit_breaker.state.changes", "name", circuitBreaker.getName(), "state", event.getStateTransition().getToState().name().toLowerCase()).increment();
                })
                .onCallNotPermitted(event -> meterRegistry.counter("circuit_breaker.calls", "name", circuitBreaker.getName(), "type", "not_permitted").increment());
    }

    // --- Core Authentication Logic (Remains the same, uses configured circuitBreaker and retryTemplate) ---

    @Around("@annotation(requiresAuthenticationAnnotation)")
    public Object authenticateAndProceed(ProceedingJoinPoint joinPoint, RequiresAuthentication requiresAuthenticationAnnotation) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getMethod().getName();

        recordAuthenticationAttempt(className, methodName);

        try {
            // 1. Extract Token from HttpServletRequest
            String token = extractBearerToken();
            if (token == null) {
                throw new UnauthorizedException("Authorization header missing or invalid");
            }

            // 2. Validate Token and Get User (with Resilience)
            // Uses Resilience4j CircuitBreaker and Spring Retry RetryTemplate
            UserDetails userDetails = circuitBreaker.executeSupplier(() -> // Execute within circuit breaker
                            retryTemplate.execute(context -> { // Execute within retry policy
                                try {
                                    // Call your token validation service (e.g., PasetoTokenService)
                                    // This service should parse, validate signature/expiry/claims, and check revocation
                                    Map<String, Object> claims = tokenValidationService.validatePublicAccessToken(token); // Or appropriate method

                                    // Extract user identifier (e.g., subject) from claims
                                    String userId = (String) claims.get("sub"); // Assuming 'sub' holds the user ID
                                    if (userId == null) {
                                        throw new TokenValidationFailureException("User identifier (sub) missing in token");
                                    }

                                    // Fetch UserDetails (which should be your User entity)
                                    // Caching should ideally happen within userService.loadUserByUsername or getUserById
                                    UserDetails loadedUser = userService.getUserById(Long.valueOf(userId)); // Or findByUsername if sub is username

                                    // Basic UserDetails checks
                                    if (!loadedUser.isEnabled()) throw new UnauthorizedException("User account is disabled");
                                    if (!loadedUser.isAccountNonLocked())
                                        throw new UnauthorizedException("User account is locked");
                                    if (!loadedUser.isAccountNonExpired())
                                        throw new UnauthorizedException("User account has expired");
                                    if (!loadedUser.isCredentialsNonExpired())
                                        throw new UnauthorizedException("User credentials have expired");

                                    return loadedUser;

                                } catch (TokenValidationFailureException | SignatureException e) {
                                    log.warn("Token validation failed during attempt {}: {}", context.getRetryCount() + 1, e.getMessage(), e);
                                    throw new UnauthorizedException("Invalid or expired token", e); // Rethrow as Unauthorized
                                } catch (Exception e) {
                                    // Handle other potential errors during validation/user fetch
                                    log.error("Error during authentication attempt {}: {}", context.getRetryCount() + 1, e.getMessage(), e);
                                    // Rethrow wrapped if necessary for retry policy
//                            throw new RuntimeException("Authentication failed during retry attempt", e);
                                    // Decide if this exception should trigger a retry based on your RetryPolicy
                                    throw e; // Rethrow to be caught by RetryTemplate
                                }
                            }, context -> { // Fallback within retry (optional, but good practice)
                                log.error("Retry attempts failed for token validation and user retrieval. Circuit breaker might open soon.");
                                throw new CircuitBreakerOpenException("Failed to validate token and retrieve user after multiple retries.");
                            })
            );

            // 3. Set Security Context
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, // Principal
                    null,        // Credentials (cleared)
                    userDetails.getAuthorities() // Authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authentication successful for user: {}", userDetails.getUsername());

            // 4. Proceed with the original method execution
            Object result = joinPoint.proceed();
            recordAuthenticationSuccess(startTime, className, methodName);
            return result;

        } catch (CircuitBreakerOpenException e) {
            log.error("Circuit breaker is open for authentication. Failing fast.", e);
            recordAuthenticationFailure(startTime, className, methodName, "circuit_breaker_open");
            throw new UnauthorizedException("Authentication service unavailable", e); // Consider a 503 Service Unavailable response
        } catch (UnauthorizedException e) {
            log.warn("Authentication failed for {}.{}: {}", className, methodName, e.getMessage());
            recordAuthenticationFailure(startTime, className, methodName, "unauthorized");
            // Check strict mode from annotation if needed, otherwise just rethrow
            if (requiresAuthenticationAnnotation.strict()) {
                throw new UnauthorizedException(requiresAuthenticationAnnotation.message(), e);
            }
            throw e; // Rethrow standard UnauthorizedException
        } catch (Throwable e) { // Catch Throwable to ensure all exceptions from the advised method are handled
            log.error("Unexpected error during authenticated method execution: {}.{}", className, methodName, e);
            recordAuthenticationFailure(startTime, className, methodName, "unexpected_error");
            throw e; // Rethrow the original exception from the method
        } finally {
            // Clear context ONLY if you are managing it entirely here and not relying on Spring Security filters
            // SecurityContextHolder.clearContext(); // Usually NOT done here if integrated with Spring Security filter chain
        }
    }

    // --- extractBearerToken (Remains the same) ---
    private String extractBearerToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("Could not retrieve request attributes to extract token.");
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.toLowerCase().startsWith(BEARER_TOKEN_PREFIX.toLowerCase())) {
            return authHeader.substring(BEARER_TOKEN_PREFIX.length());
        }
        log.trace("No Bearer token found in Authorization header.");
        return null;
    }

    // --- Metrics Recording Helpers (Remains the same) ---

    private void recordAuthenticationAttempt(String className, String methodName) {
        meterRegistry.counter("authentication.attempts", "class", className, "method", methodName).increment();
    }

    private void recordAuthenticationSuccess(long startTime, String className, String methodName) {
        recordAuthenticationMetrics(startTime, className, methodName, true, "success");
    }

    private void recordAuthenticationFailure(long startTime, String className, String methodName, String reason) {
        recordAuthenticationMetrics(startTime, className, methodName, false, reason);
    }

    private void recordAuthenticationMetrics(long startTime, String className, String methodName, boolean success, String outcome) {
        long duration = System.currentTimeMillis() - startTime;
        meterRegistry.timer("authentication.duration",
                        "class", className,
                        "method", methodName,
                        "success", String.valueOf(success),
                        "outcome", outcome)
                .record(duration, TimeUnit.MILLISECONDS);

        meterRegistry.counter("authentication.outcome",
                        "class", className,
                        "method", methodName,
                        "success", String.valueOf(success),
                        "outcome", outcome)
                .increment();
    }

    // --- Exception Handlers (Remains the same) ---

    @ExceptionHandler(AuthenticationException.class) // Spring Security's base exception
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.error("Authentication failed: {}", ex.getMessage());
        meterRegistry.counter("authentication.handler.failures", "type", "authentication_exception").increment();
        return createErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class) // Your custom exception
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
        log.warn("Authorization failed: {}", ex.getMessage()); // Log as WARN for authorization issues
        meterRegistry.counter("authentication.handler.failures", "type", "unauthorized").increment();
        return createErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage()); // Return 401 for unauthorized
    }

    @ExceptionHandler(Exception.class) // Generic fallback
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error during authentication/authorization aspect: {}", ex.getMessage(), ex);
        meterRegistry.counter("authentication.handler.failures", "type", "unexpected").increment();
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred during security processing.");
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(HttpStatus status, String detail) {
        // Ensure the status code is appropriate for the error type.
        // For generic exceptions, INTERNAL_SERVER_ERROR (500) is usually correct.
        // For AuthenticationException and UnauthorizedException, UNAUTHORIZED (401) is correct.
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR; // Default to 500 if status is null
        }

        HttpStatus finalStatus = status;
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse() {
                    @NotNull
                    @Override
                    public HttpStatusCode getStatusCode() {
                        return finalStatus;
                    }

                    @NotNull
                    @Override
                    public ProblemDetail getBody() {
                        return ProblemDetail.forStatusAndDetail(finalStatus, detail);
                    }
                });
    }
}
