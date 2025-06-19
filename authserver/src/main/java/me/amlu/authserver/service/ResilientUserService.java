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

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import me.amlu.authserver.exceptions.PasswordMismatchException;
import me.amlu.authserver.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A resilient wrapper for UserManager service.
 * <p>
 * This service adds resilience patterns to user management operations:
 * <ul>
 *   <li>Circuit breakers to prevent cascading failures</li>
 *   <li>Bulkheads to isolate user operations</li>
 *   <li>Retry logic for transient failures</li>
 *   <li>Time limiters to prevent long-running operations</li>
 *   <li>Fallback mechanisms for graceful degradation</li>
 * </ul>
 * </p>
 */

@Service
public class ResilientUserService {
    private static final Logger log = LoggerFactory.getLogger(ResilientUserService.class);
    private static final String USER_SERVICE = "userService";

    private final Executor executor = Executors.newCachedThreadPool();
    private final me.amlu.authserver.user.service.UserManager userManager;

    public ResilientUserService(me.amlu.authserver.user.service.UserManager userManager) {
        this.userManager = userManager;
    }

    /**
     * Creates a user with resilience patterns.
     * <p>
     * This method wraps the UserManager's createUser method with:
     * <ul>
     *   <li>Circuit breaker to prevent cascading failures</li>
     *   <li>Bulkhead to limit concurrent user creation operations</li>
     *   <li>Retry logic for transient failures</li>
     * </ul>
     * </p>
     *
     * @param givenName        User's first name
     * @param middleName       User's middle name (optional)
     * @param surname          User's last name
     * @param nickname         User's preferred name (optional)
     * @param email            User's primary email address
     * @param rawPassword      User's password in plain text (will be hashed)
     * @param mobileNumber     User's mobile phone number (optional)
     * @param defaultRegion    Default region code for phone number formatting
     * @param recoveryEmailRaw Secondary email for account recovery (optional)
     * @return The newly created User entity
     * @throws RuntimeException If user creation fails and cannot be retried
     */
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "createUserFallback")
    @Bulkhead(name = USER_SERVICE)
    @Retry(name = USER_SERVICE)
    public User createUser(String givenName, String middleName, String surname, String nickname,
                           String email, String rawPassword, String mobileNumber,
                           String defaultRegion, String recoveryEmailRaw) {
        return userManager.createUser(givenName, middleName, surname, nickname,
                email, rawPassword, mobileNumber,
                defaultRegion, recoveryEmailRaw);
    }

    /**
     * Fallback method for user creation failures.
     * <p>
     * This method is called when the circuit breaker detects a failure in createUser.
     * It logs the error and throws a user-friendly exception.
     * </p>
     *
     * @param givenName        User's first name
     * @param middleName       User's middle name (optional)
     * @param surname          User's last name
     * @param nickname         User's preferred name (optional)
     * @param email            User's primary email address
     * @param rawPassword      User's password in plain text
     * @param mobileNumber     User's mobile phone number (optional)
     * @param defaultRegion    Default region code for phone number formatting
     * @param recoveryEmailRaw Secondary email for account recovery (optional)
     * @param e                The exception that occurred
     * @return Never returns a value, always throws an exception
     * @throws RuntimeException A user-friendly exception
     */
    private User createUserFallback(String givenName, String middleName, String surname, String nickname,
                                    String email, String rawPassword, String mobileNumber,
                                    String defaultRegion, String recoveryEmailRaw, Exception e) {
        log.error("Failed to create user: {}", email, e);
        throw new RuntimeException("User creation service is currently unavailable. Please try again later.", e);
    }

    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "changePasswordFallback")
    @Bulkhead(name = USER_SERVICE)
    @Retry(name = USER_SERVICE)
    public void changeUserPassword(Long userId, String oldRawPassword, String newRawPassword) {
        userManager.changeUserPassword(userId, oldRawPassword, newRawPassword);
    }

    private void changePasswordFallback(Long userId, String oldRawPassword, String newRawPassword, Exception e) {
        log.error("Failed to change password for user ID: {}", userId, e);
        if (e instanceof PasswordMismatchException) {
            throw (PasswordMismatchException) e;
        }
        throw new RuntimeException("Password change service is currently unavailable. Please try again later.", e);
    }

    /**
     * Updates a user profile asynchronously with resilience patterns.
     * <p>
     * This method provides an asynchronous version of the profile update operation with:
     * <ul>
     *   <li>Circuit breaker to prevent cascading failures</li>
     *   <li>Time limiter to cancel long-running operations</li>
     *   <li>Thread pool bulkhead to limit concurrent profile updates</li>
     * </ul>
     * </p>
     *
     * @param userId              The ID of the user to update
     * @param newGivenName        New first name (optional)
     * @param newMiddleName       New middle name (optional)
     * @param newSurname          New last name (optional)
     * @param newNickname         New nickname (optional)
     * @param newMobileNumber     New mobile number (optional)
     * @param defaultRegion       Default region code for phone number formatting
     * @param newRecoveryEmailRaw New recovery email (optional)
     * @return A CompletableFuture that will complete with the updated User
     */
    @CircuitBreaker(name = USER_SERVICE)
    @TimeLimiter(name = USER_SERVICE)
    @Bulkhead(name = USER_SERVICE, type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<User> updateUserProfileAsync(Long userId, String newGivenName, String newMiddleName,
                                                          String newSurname, String newNickname,
                                                          String newMobileNumber, String defaultRegion,
                                                          String newRecoveryEmailRaw) {
        return CompletableFuture.supplyAsync(() ->
                userManager.updateUserProfile(userId, newGivenName, newMiddleName, newSurname,
                        newNickname, newMobileNumber, defaultRegion,
                        newRecoveryEmailRaw), executor);
    }
}
