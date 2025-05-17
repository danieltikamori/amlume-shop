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
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.user_management.dto.UserRegistrationRequest;
import org.springframework.transaction.annotation.Transactional;

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
     * @throws TooManyAttemptsException   If registration attempts does exceed limits (local check).
     * @throws InvalidCaptchaException    If captcha validation fails (local check).
     * @throws UserAlreadyExistsException If the username or userEmail is already taken (reported by authserver).
     * @throws UserRegistrationException  If an internal error occurs during registration (local or authserver).
     * @throws IllegalArgumentException   If the request is invalid (reported by authserver).
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
}
