/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.auth.service;

import jakarta.validation.Valid;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.auth.dto.AuthResponse;
import me.amlu.shop.amlume_shop.auth.dto.LoginRequest;
import me.amlu.shop.amlume_shop.user_management.dto.UserRegistrationRequest;
import me.amlu.shop.amlume_shop.user_management.AuthenticationInfo;
import me.amlu.shop.amlume_shop.user_management.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static me.amlu.shop.amlume_shop.commons.Constants.AUTH_CACHE;

/**
 * Interface defining core authentication and related security operations.
 */
public interface AuthenticationInterface {

    // --- Registration ---

    /**
     * Registers a new user by delegating to the central authentication server.
     *
     * @param request   The user registration details.
     * @param ipAddress The IP address of the registration request.
     * @throws TooManyAttemptsException    If registration attempts does exceed limits (local check).
     * @throws InvalidCaptchaException     If captcha validation fails (local check).
     * @throws UserAlreadyExistsException  If the username or email is already taken (reported by authserver).
     * @throws UserRegistrationException   If an internal error occurs during registration (local or authserver).
     * @throws IllegalArgumentException    If the request is invalid (reported by authserver).
     */
    // CHANGED: Return type is now void
    void register(@Valid UserRegistrationRequest request, String ipAddress) throws TooManyAttemptsException, InvalidCaptchaException, UserAlreadyExistsException, UserRegistrationException, IllegalArgumentException;

    /**
     * Authenticates a user based on login credentials.
     * Handles password checking, account status, and initiates MFA if required.
     *
     * @param request   The login request details.
     * @param ipAddress The IP address of the login request.
     * @return AuthResponse indicating success, failure, or MFA requirement.
     * @throws TooManyAttemptsException If login attempts does exceed limits (for user or IP).
     * @throws InvalidCaptchaException  If captcha validation fails.
     * @throws AuthenticationFailException If authentication fails due to credentials, locking, or internal errors.
     */
    // This method will likely become obsolete as login is handled by oauth2Login()
//    @Deprecated(since = "2025-05-15", forRemoval = true)
//    AuthResponse authenticateUser(LoginRequest request, String ipAddress) throws TooManyAttemptsException, InvalidCaptchaException, AuthenticationFailException;

    // --- Logout ---

    /**
     * Logs out a user by revoking their tokens.
     *
     * @param accessToken  The access token to revoke.
     * @param refreshToken The refresh token to revoke.
     */
    @Transactional
    void logout(String accessToken, String refreshToken);

    // --- Login Flow Helpers ---

    /**
     * Handles the final steps after successful authentication (password + MFA if applicable).
     * Resets failed attempts, updates last login, generates tokens, and associates device fingerprint.
     *
     * @param user              The successfully authenticated user.
     * @param ipAddress         The IP address of the login request.
     * @param deviceFingerprint The generated device fingerprint for this login.
     * @return AuthResponse containing the access and refresh tokens.
     */
    // This method is part of the old PASETO flow and will be superseded by OAuth2/OIDC login handling
//    @Deprecated(since = "2025-05-15", forRemoval = true)
//    AuthResponse handleSuccessfulLogin(User user, String ipAddress, String deviceFingerprint);

    /**
     * Handles actions upon a failed login attempt (e.g., incorrect password).
     * Increments failed attempts, potentially locks the account, and audits the event.
     *
     * @param user      The user for whom the login failed.
     * @param ipAddress The IP address of the failed login attempt.
     */
    // This method is part of the old PASETO flow and will be superseded by authserver's event handling
//    @Deprecated(since = "2025-05-15", forRemoval = true)
//    void handleFailedLogin(User user, String ipAddress);

    /**
     * Resets the failed login attempt counter for a user (in DB and cache).
     *
     * @param user The user whose attempts should be reset.
     */
    // This method is part of the old PASETO flow and will be superseded by authserver's UserManager
//    @Deprecated(since = "2025-05-15", forRemoval = true)
//    void resetFailedAttempts(User user);

    /**
     * Increments the failed login attempt counter for a user (in DB and cache).
     *
     * @param user The user whose attempts should be incremented.
     */
    // This method is part of the old PASETO flow and will be superseded by authserver's UserManager
//    @Deprecated(since = "2025-05-15", forRemoval = true)
//    void increaseFailedAttempts(User user);

    /**
     * Locks a user account in the database.
     *
     * @param user The user to lock.
     */
    // This method is part of the old PASETO flow and will be superseded by authserver's UserManager
//    @Deprecated(since = "2025-05-15", forRemoval = true)
//    void lockUser(User user);

    /**
     * Manually unlocks a user account.
     *
     * @param username The username of the account to unlock.
     */
    // This method is part of the old PASETO flow and will be superseded by authserver's UserManager
//    @Deprecated(since = "2025-05-15", forRemoval = true)
//    void unlockUser(String username);

    /**
     * Checks if a locked user account's lock duration has expired and unlocks it if so.
     *
     * @param user The user to check.
     * @return true if the account was unlocked or was already unlocked, false if it remains locked.
     */
    // This method is part of the old PASETO flow and will be superseded by authserver's UserManager
//    @Deprecated(since = "2025-05-15", forRemoval = true)
//    boolean unlockWhenTimeExpired(User user);

    // --- Device Fingerprinting ---

    /**
     * Generates a device fingerprint based on request details.
     *
     * @param userAgent    The User-Agent string.
     * @param screenWidth  The screen width.
     * @param screenHeight The screen height.
     * @return The generated device fingerprint string, or null if generation fails.
     */
    // This can potentially remain if amlume-shop needs its own device tracking for shop-specific actions
    String generateAndHandleFingerprint(String userAgent, String screenWidth, String screenHeight);

    // --- Token Information ---

    /**
     * Determines the scope (roles/authorities) for the currently authenticated user.
     *
     * @return A space-separated string of authorities.
     */
    // This method will need to be adapted to read claims from the JWT issued by authserver
    String determineUserScope();

    /**
     * Gets the configured duration for access tokens.
     *
     * @return The access token duration.
     */
    // This is for the old PASETO tokens, will be superseded by authserver's token settings
//    @Deprecated(since = "2025-05-15", forRemoval = true)
//    Duration getAccessTokenDuration();

    /**
     * Gets the configured duration for refresh tokens.
     *
     * @return The refresh token duration.
     */
    // This is for the old PASETO tokens, will be superseded by authserver's token settings
//    @Deprecated(since = "2025-05-15", forRemoval = true)
//    Duration getRefreshTokenDuration();

    /**
     * Gets the configured duration for the JTI (token ID) blocklist entry.
     *
     * @return The JTI duration.
     */
    // This is for the old PASETO tokens, will be superseded by authserver's token settings
//    @Deprecated(since = "2025-05-15", forRemoval = true)
//    Duration getJtiDuration();

    // --- Authentication Info Caching ---

    /**
     * Retrieves authentication information (like password hash, lock status) for a user, potentially from cache.
     *
     * @param username The username to retrieve info for.
     * @return The AuthenticationInfo object.
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException if the user is not found.
     */
    // This is for the old PASETO flow and local password management, will be superseded
//    @Deprecated(since = "2025-05-15", forRemoval = true)
//    @Transactional(readOnly = true)
//    AuthenticationInfo getAuthenticationInfo(String username);

    /**
     * Updates the authentication information for a user and evicts the corresponding cache entry.
     *
     * @param username The username whose info is being updated.
     * @param newInfo  The new AuthenticationInfo object.
     */
    // This is for the old PASETO flow and local password management, will be superseded
//    @Deprecated(since = "2025-05-15", forRemoval = true)
//    @Transactional(noRollbackFor = Exception.class) // Allow commit even if cache eviction fails? Review this.
//    @CacheEvict(value = AUTH_CACHE, key = "'" + Constants.AUTH_CACHE_KEY_PREFIX + "' + #username") // Use SpEL for key prefix
//    void updateAuthenticationInfo(String username, AuthenticationInfo newInfo);

    // --- DEPRECATED ---
    // @Deprecated(since = "2025-04-20", forRemoval = true)
    // void handleLockedAccount(User user, String ipAddress) throws LockedException;
}
