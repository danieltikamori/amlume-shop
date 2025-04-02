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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.lettuce.core.RedisConnectionException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.CircuitBreakerOpenException;
import me.amlu.shop.amlume_shop.exceptions.UnauthorizedException;
import me.amlu.shop.amlume_shop.payload.UserDTO;
import me.amlu.shop.amlume_shop.security.service.UserServiceImpl;
import me.amlu.shop.amlume_shop.user_management.User;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static me.amlu.shop.amlume_shop.commons.Constants.*;

/**
 * Aspect responsible for handling authentication and authorization.
 * Implements circuit breaker pattern for resilience and caching for performance.
 *
 * @see RequiresAuthentication
 * @see CircuitBreaker
 * @see RetryTemplate
 */
@Slf4j
@Aspect
@Component
@Order(1) // Ensure authentication check runs before other aspects
public class AuthenticationAspect {

    private final UserServiceImpl userService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final CircuitBreaker circuitBreaker;
    private final RetryTemplate retryTemplate;

    public AuthenticationAspect(
            @NonNull UserServiceImpl userService,
            @NonNull StringRedisTemplate redisTemplate,
            @NonNull ObjectMapper objectMapper,
            @NonNull MeterRegistry meterRegistry) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.circuitBreaker = createCircuitBreaker();
        this.retryTemplate = createRetryTemplate();
    }

    private void recordMetrics(String methodName, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        meterRegistry.timer("authentication.execution.time",
                        Tags.of("method", methodName))
                .record(executionTime, TimeUnit.MILLISECONDS);
    }

    // Add circuit breaker metrics
    private void configureCircuitBreakerMetrics(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
                .onSuccess(event -> meterRegistry.counter("circuit_breaker.success").increment())
                .onError(event -> meterRegistry.counter("circuit_breaker.error").increment())
                .onStateTransition(event ->
                        meterRegistry.counter("circuit_breaker.state." + event.getStateTransition()).increment());
    }

    private CircuitBreaker createCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(10)
                .slidingWindowSize(100)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(10)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(
                        IOException.class,
                        TimeoutException.class,
                        RedisConnectionException.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class
                )
                .build();

        CircuitBreaker authenticationCircuitBreaker = CircuitBreaker.of("authenticationCircuitBreaker", config);
        configureCircuitBreakerMetrics(authenticationCircuitBreaker);
        return authenticationCircuitBreaker;
    }

    private RetryTemplate createRetryTemplate() {
        RetryTemplate localRetryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(MAX_RETRY_ATTEMPTS);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(RETRY_INTERVAL);

        localRetryTemplate.setRetryPolicy(retryPolicy);
        localRetryTemplate.setBackOffPolicy(backOffPolicy);

        return localRetryTemplate;
    }

    @PostConstruct
    private void validateConfiguration() {
        if (ANNOTATION_CACHE_DURATION.isNegative() || ANNOTATION_CACHE_DURATION.isZero()) {
            throw new IllegalStateException("Invalid annotation cache duration");
        }
        if (USER_CACHE_DURATION.isNegative() || USER_CACHE_DURATION.isZero()) {
            throw new IllegalStateException("Invalid user cache duration");
        }
    }

    // Metrics for monitoring cache performance
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void reportCacheMetrics() {
        try {
            Long annotationCacheSize = (long) Objects.requireNonNull(redisTemplate.keys(ANNOTATION_CACHE_KEY_PREFIX + "*")).size();
            Long userCacheSize = (long) Objects.requireNonNull(redisTemplate.keys(USER_CACHE_KEY_PREFIX + "*")).size();

            meterRegistry.gauge("cache.size", Tags.of("type", "annotation"), annotationCacheSize);
            meterRegistry.gauge("cache.size", Tags.of("type", "user"), userCacheSize);
        } catch (Exception e) {
            log.error("Failed to report cache metrics", e);
        }
    }

    // Missing main aspect method for authentication
    @Around("@annotation(requiresAuthentication)")
    public Object authenticate(ProceedingJoinPoint joinPoint, RequiresAuthentication requiresAuthentication) throws Throwable {
        try {
            String token = extractToken();
            if (token == null) {
                throw new UnauthorizedException("No authentication token provided");
            }

            return circuitBreaker.executeSupplier(() ->
                    {
                        try {
                            return retryTemplate.execute(retryContext -> {
                                validateToken(token);
                                return joinPoint.proceed();
                            });
                        } catch (Throwable e) {
                            throw new CircuitBreakerOpenException(e);
                        }
                    }
            );
        } catch (Exception e) {
            log.error("Authentication failed", e);
            meterRegistry.counter("authentication.failures").increment();
            throw new UnauthorizedException("Authentication failed");
        }
    }

    private String extractToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void validateToken(String token) {
        String userKey = USER_CACHE_KEY_PREFIX + token;
        String userData = redisTemplate.opsForValue().get(userKey);

        if (userData == null) {
            throw new UnauthorizedException("Invalid or expired token");
        }

        try {
            UserDTO user = objectMapper.readValue(userData, UserDTO.class);
            if (!userService.isValidUser(user)) {
                throw new UnauthorizedException("Invalid user");
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing user data", e);
            throw new UnauthorizedException("Invalid user data");
        }
    }

    @Cacheable(value = "authAnnotations",
            key = "#method.declaringClass.name + '.' + #method.name",
            unless = "#result == null",
            cacheManager = "timeoutCacheManager")
    public RequiresAuthentication getCachedAnnotation(Method method) {
        return method.getAnnotation(RequiresAuthentication.class);
    }

    private void cacheAuthenticationResult(User user, String cacheKey) {
        try {
            String userJson = objectMapper.writeValueAsString(user);
            redisTemplate.opsForValue().set(cacheKey, userJson, USER_CACHE_DURATION);
            meterRegistry.counter("cache.stores", "type", "user").increment();
        } catch (JsonProcessingException e) {
            log.error("Failed to cache user authentication result", e);
            meterRegistry.counter("cache.errors", "type", "user").increment();
        }
    }


    // TODO: simplify this method
    @Before("@annotation(requiresAuthentication) || @within(requiresAuthentication)")
    public void checkAuthentication(JoinPoint joinPoint, RequiresAuthentication requiresAuthentication)
            throws UnauthorizedException {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = null;
        Method method = null;
        String className = "unknown";
        String methodName = "unknown";

        try {
            // Extract method information with validation
            // Extract and validate method signature
            signature = extractMethodSignature(joinPoint);
            method = signature.getMethod();
            className = signature.getDeclaringType().getSimpleName();
            methodName = method.getName();

            // Record method invocation metric
            meterRegistry.counter("authentication.attempts",
                    "class", className,
                    "method", methodName).increment();

            // Get cached annotation using circuit breaker pattern
            Method finalMethod = method;
            RequiresAuthentication authAnnotation = circuitBreaker.executeSupplier(() ->
                    retryTemplate.execute(context -> getOrCacheAnnotation(finalMethod)));

            // Validate user authentication with circuit breaker and retry
            User user = circuitBreaker.executeSupplier(() ->
                    retryTemplate.execute(context -> getOrFetchAuthenticatedUser()));

            // Log success and record metrics
            logAuthenticationSuccess(className, methodName);
            recordAuthenticationMetrics(startTime, className, methodName, true);

        } catch (UnauthorizedException e) {
            handleAuthenticationFailure(
                    signature != null ? signature.getDeclaringType().getSimpleName() : "unknown",
                    method != null ? method.getName() : "unknown",
                    e,
                    requiresAuthentication
            );
            recordAuthenticationMetrics(startTime,
                    signature != null ? signature.getDeclaringType().getSimpleName() : "unknown",
                    method != null ? method.getName() : "unknown",
                    false);
            throw e;
        } catch (Exception e) {
            handleUnexpectedError(
                    signature != null ? signature.getDeclaringType().getSimpleName() : "unknown",
                    method != null ? method.getName() : "unknown",
                    e,
                    requiresAuthentication
            );
            recordAuthenticationMetrics(startTime,
                    signature != null ? signature.getDeclaringType().getSimpleName() : "unknown",
                    method != null ? method.getName() : "unknown",
                    false);
            throw new UnauthorizedException("Authentication failed due to unexpected error", e);
        }
    }

    private MethodSignature extractMethodSignature(JoinPoint joinPoint) throws UnauthorizedException {
        try {
            return (MethodSignature) joinPoint.getSignature();
        } catch (ClassCastException e) {
            log.error("Failed to cast join point signature to MethodSignature", e);
            meterRegistry.counter("authentication.errors", "type", "signature_cast_error").increment();
            throw new UnauthorizedException("Invalid method signature");
        }
    }

    private void logAuthenticationSuccess(String className, String methodName) {
        if (log.isDebugEnabled()) {
            log.debug("Authentication successful for method: {}.{}", className, methodName);
        }
    }

    private void recordAuthenticationAttempt(String className, String methodName) {
        meterRegistry.counter("authentication.attempts",
                        "class", className,
                        "method", methodName)
                .increment();
    }

    private void recordAuthenticationSuccess(long startTime, String className, String methodName) {
        recordAuthenticationMetrics(startTime, className, methodName, true);
    }

    private void recordAuthenticationFailure(long startTime, String className, String methodName) {
        recordAuthenticationMetrics(startTime, className, methodName, false);
    }

    private void recordAuthenticationMetrics(long startTime, String className, String methodName, boolean success) {
        long duration = System.currentTimeMillis() - startTime;
        meterRegistry.timer("authentication.duration",
                        "class", className,
                        "method", methodName,
                        "success", String.valueOf(success))
                .record(duration, TimeUnit.MILLISECONDS);

        if (success) {
            meterRegistry.counter("authentication.success",
                            "class", className,
                            "method", methodName)
                    .increment();
        } else {
            meterRegistry.counter("authentication.failure",
                            "class", className,
                            "method", methodName)
                    .increment();
        }
    }

    @Cacheable(value = "authAnnotations",
            key = "#method.declaringClass.name + '.' + #method.name",
            unless = "#result == null",
            cacheManager = "timeoutCacheManager")
    private RequiresAuthentication getOrCacheAnnotation(Method method) {
        String cacheKey = ANNOTATION_CACHE_KEY_PREFIX + method.toString();

        try {
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                return objectMapper.readValue(cachedValue, RequiresAuthentication.class);
            }

            RequiresAuthentication annotation = method.getAnnotation(RequiresAuthentication.class);
            if (annotation == null) {
                annotation = method.getDeclaringClass().getAnnotation(RequiresAuthentication.class);
            }

            if (annotation != null) {
                String serializedAnnotation = objectMapper.writeValueAsString(annotation);
                redisTemplate.opsForValue().set(cacheKey, serializedAnnotation, ANNOTATION_CACHE_DURATION);
            }

            return annotation;
        } catch (JsonProcessingException e) {
            log.warn("Failed to process annotation cache for method: {}", method.getName(), e);
            return method.getAnnotation(RequiresAuthentication.class);
        } catch (RedisConnectionException e) {
            log.warn("Redis cache unavailable for method: {}", method.getName(), e);
            return method.getAnnotation(RequiresAuthentication.class);
        }
    }

    private User getOrFetchAuthenticatedUser() {
        String cacheKey = USER_CACHE_KEY_PREFIX + SecurityContextHolder.getContext().getAuthentication().getName();

        return circuitBreaker.executeSupplier(() ->
                retryTemplate.execute(context -> {
                    try {
                        Timer.Sample sample = Timer.start(meterRegistry);
                        String cachedUser = redisTemplate.opsForValue().get(cacheKey);

                        if (cachedUser != null) {
                            sample.stop(meterRegistry.timer("cache.hits", "type", "user"));
                            return objectMapper.readValue(cachedUser, User.class);
                        }

                        sample.stop(meterRegistry.timer("cache.misses", "type", "user"));
                        User currentUser = userService.getCurrentUser();
                        cacheAuthenticationResult(currentUser, cacheKey);
                        return currentUser;

                    } catch (Exception e) {
                        meterRegistry.counter("cache.errors", "type", "user").increment();
                        throw new UnauthorizedException("Failed to authenticate user", e);
                    }
                })
        );
    }

    private String generateAuthCacheKey() {
        return AUTH_CACHE_KEY_PREFIX + SecurityContextHolder.getContext().getAuthentication().getName()
                + ":" + Thread.currentThread().threadId();
    }

    private void handleAuthenticationFailure(String className, String methodName,
                                             UnauthorizedException e, RequiresAuthentication annotation) {
        log.warn("Authentication failed for {}.{}: {}", className, methodName, e.getMessage());

        if (annotation.strict()) {
            throw new UnauthorizedException(annotation.message(), e);
        }
    }

    private void handleUnexpectedError(String className, String methodName,
                                       Exception e, RequiresAuthentication annotation) {
        log.error("Unexpected error during authentication check for {}.{}",
                className, methodName, e);

        if (annotation.strict()) {
            throw new UnauthorizedException(annotation.message(), e);
        }
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.error("Authentication failed: {}", ex.getMessage());
        meterRegistry.counter("authentication.failures").increment();
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse() {
                    @NotNull
                    @Override
                    public HttpStatusCode getStatusCode() {
                        return HttpStatus.UNAUTHORIZED;
                    }

                    @NotNull
                    @Override
                    public ProblemDetail getBody() {
                        return ProblemDetail.forStatusAndDetail(
                                HttpStatus.UNAUTHORIZED, ex.getMessage());
                    }
                });
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error during authentication: {}", ex.getMessage(), ex);
        meterRegistry.counter("authentication.errors").increment();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse() {
                    @NotNull
                    @Override
                    public HttpStatusCode getStatusCode() {
                        return HttpStatus.INTERNAL_SERVER_ERROR;
                    }

                    @NotNull
                    @Override
                    public ProblemDetail getBody() {
                        return ProblemDetail.forStatusAndDetail(
                                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
                    }
                });
    }


    @Before("@annotation(requiresRole)")
    public void checkRole(JoinPoint joinPoint, RequiresRole requiresRole)
            throws UnauthorizedException {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getMethod().getName();
            String className = signature.getDeclaringType().getSimpleName();

            log.debug("Checking role '{}' for method: {}.{}",
                    requiresRole.value(), className, methodName);

            var user = userService.getCurrentUser();
            if (!userService.hasRole(user, requiresRole.value())) {
                log.warn("Role check failed for user {} accessing {}.{}",
                        user.getUsername(), className, methodName);
                throw new UnauthorizedException("Required role: " + requiresRole.value());
            }

            log.debug("Role check successful for method: {}.{}", className, methodName);
        } catch (UnauthorizedException e) {
            log.warn("Role check failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during role check", e);
            throw new UnauthorizedException("Role check failed due to system error");
        }
    }


    @Scheduled(fixedRate = 3600000) // Every hour
    public void evictExpiredCaches() {
        try {
            Set<String> keys = redisTemplate.keys(ANNOTATION_CACHE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                // Add metrics
                meterRegistry.counter("cache.evictions").increment(keys.size());
                log.info("Evicted {} expired annotation cache entries", keys.size());
            }
        } catch (Exception e) {
            log.error("Failed to evict expired caches", e);
            meterRegistry.counter("cache.eviction.failures").increment();
        }
    }
}