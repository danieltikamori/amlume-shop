/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.security.service;

import jakarta.validation.Valid;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.security.model.MfaToken;
import me.amlu.shop.amlume_shop.auth_management.dto.AuthResponse;
import me.amlu.shop.amlume_shop.auth_management.dto.LoginRequest;
import me.amlu.shop.amlume_shop.security.dto.MfaVerificationRequest;
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
     * Registers a new user.
     *
     * @param request   The user registration details.
     * @param ipAddress The IP address of the registration request.
     * @return AuthResponse containing tokens and registration status.
     * @throws TooManyAttemptsException    If registration attempts does exceed limits.
     * @throws InvalidCaptchaException     If captcha validation fails.
     * @throws UserAlreadyExistsException  If the username or email is already taken.
     * @throws UserRegistrationException   If an internal error occurs during registration.
     */
    AuthResponse register(@Valid UserRegistrationRequest request, String ipAddress) throws TooManyAttemptsException, InvalidCaptchaException, UserAlreadyExistsException, UserRegistrationException;

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
    AuthResponse authenticateUser(LoginRequest request, String ipAddress) throws TooManyAttemptsException, InvalidCaptchaException, AuthenticationFailException;

    // --- Logout ---

    /**
     * Logs out a user by revoking their tokens.
     *
     * @param accessToken  The access token to revoke.
     * @param refreshToken The refresh token to revoke.
     */
    @Transactional
    void logout(String accessToken, String refreshToken);

    // --- MFA ---

    /**
     * Initializes an MFA token entity for a user (usually during first MFA setup).
     *
     * @param user The user to initialize MFA for.
     * @return The newly created MfaToken entity.
     */
    MfaToken initializeMfaToken(User user);

    /**
     * Initiates the MFA challenge phase for a user.
     * Returns details for MFA setup (QR code) or just indicates MFA is required.
     *
     * @param user The user undergoing MFA.
     * @return AuthResponse indicating MFA requirement and potentially setup details.
     */
    AuthResponse initiateMfaChallenge(User user);

    /**
     * Verifies the provided MFA code and completes the login process if valid.
     *
     * @param request   The MFA verification request containing username and code.
     * @param ipAddress The IP address of the verification request.
     * @return AuthResponse containing tokens upon successful verification.
     * @throws TooManyAttemptsException If MFA verification attempts does exceed limits.
     * @throws AuthenticationFailException If verification fails due to invalid code, locking, or internal errors.
     */
    AuthResponse verifyMfaAndLogin(MfaVerificationRequest request, String ipAddress) throws TooManyAttemptsException, AuthenticationFailException;

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
    AuthResponse handleSuccessfulLogin(User user, String ipAddress, String deviceFingerprint);

    /**
     * Handles actions upon a failed login attempt (e.g., incorrect password).
     * Increments failed attempts, potentially locks the account, and audits the event.
     *
     * @param user      The user for whom the login failed.
     * @param ipAddress The IP address of the failed login attempt.
     */
    void handleFailedLogin(User user, String ipAddress);

    /**
     * Resets the failed login attempt counter for a user (in DB and cache).
     *
     * @param user The user whose attempts should be reset.
     */
    void resetFailedAttempts(User user);

    /**
     * Increments the failed login attempt counter for a user (in DB and cache).
     *
     * @param user The user whose attempts should be incremented.
     */
    void increaseFailedAttempts(User user);

    /**
     * Locks a user account in the database.
     *
     * @param user The user to lock.
     */
    void lockUser(User user);

    /**
     * Manually unlocks a user account.
     *
     * @param username The username of the account to unlock.
     */
    void unlockUser(String username);

    /**
     * Checks if a locked user account's lock duration has expired and unlocks it if so.
     *
     * @param user The user to check.
     * @return true if the account was unlocked or was already unlocked, false if it remains locked.
     */
    boolean unlockWhenTimeExpired(User user);

    // --- Device Fingerprinting ---

    /**
     * Generates a device fingerprint based on request details.
     *
     * @param userAgent    The User-Agent string.
     * @param screenWidth  The screen width.
     * @param screenHeight The screen height.
     * @return The generated device fingerprint string, or null if generation fails.
     */
    String generateAndHandleFingerprint(String userAgent, String screenWidth, String screenHeight);

    // --- Token Information ---

    /**
     * Determines the scope (roles/authorities) for the currently authenticated user.
     *
     * @return A space-separated string of authorities.
     */
    String determineUserScope();

    /**
     * Gets the configured duration for access tokens.
     *
     * @return The access token duration.
     */
    Duration getAccessTokenDuration();

    /**
     * Gets the configured duration for refresh tokens.
     *
     * @return The refresh token duration.
     */
    Duration getRefreshTokenDuration();

    /**
     * Gets the configured duration for the JTI (token ID) blocklist entry.
     *
     * @return The JTI duration.
     */
    Duration getJtiDuration();

    // --- Authentication Info Caching ---

    /**
     * Retrieves authentication information (like password hash, lock status) for a user, potentially from cache.
     *
     * @param username The username to retrieve info for.
     * @return The AuthenticationInfo object.
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException if the user is not found.
     */
    @Transactional(readOnly = true)
    AuthenticationInfo getAuthenticationInfo(String username);

    /**
     * Updates the authentication information for a user and evicts the corresponding cache entry.
     *
     * @param username The username whose info is being updated.
     * @param newInfo  The new AuthenticationInfo object.
     */
    @Transactional(noRollbackFor = Exception.class) // Allow commit even if cache eviction fails? Review this.
    @CacheEvict(value = AUTH_CACHE, key = "'" + Constants.AUTH_CACHE_KEY_PREFIX + "' + #username") // Use SpEL for key prefix
    void updateAuthenticationInfo(String username, AuthenticationInfo newInfo);

    // --- DEPRECATED ---
    // @Deprecated(since = "2025-04-20", forRemoval = true)
    // void handleLockedAccount(User user, String ipAddress) throws LockedException;
}
