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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.model.MfaToken;
import me.amlu.shop.amlume_shop.model.UserDeviceFingerprint;
import me.amlu.shop.amlume_shop.payload.user.AuthResponse;
import me.amlu.shop.amlume_shop.payload.user.LoginRequest;
import me.amlu.shop.amlume_shop.payload.user.MfaVerificationRequest;
import me.amlu.shop.amlume_shop.payload.user.UserRegistrationRequest;
import me.amlu.shop.amlume_shop.repositories.MfaTokenRepository;
import me.amlu.shop.amlume_shop.repositories.UserDeviceFingerprintRepository;
import me.amlu.shop.amlume_shop.security.failedlogin.FailedLoginAttemptService;
import me.amlu.shop.amlume_shop.security.paseto.PasetoTokenService;
import me.amlu.shop.amlume_shop.security.paseto.TokenRevocationService;
import me.amlu.shop.amlume_shop.security.paseto.util.TokenConstants;
import me.amlu.shop.amlume_shop.service.CacheService;
import me.amlu.shop.amlume_shop.user_management.AuthenticationInfo;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserRepository;
import me.amlu.shop.amlume_shop.user_management.UserService;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static me.amlu.shop.amlume_shop.commons.Constants.AUTH_CACHE;
import static me.amlu.shop.amlume_shop.commons.Constants.AUTH_CACHE_KEY_PREFIX;

@Service
@Transactional
public class UserAuthenticator implements AuthenticationInterface {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UserAuthenticator.class);

    private final CacheService cacheService;
    private final CaptchaService captchaService;
    private final MeterRegistry meterRegistry;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MfaService mfaService;
    private final MfaTokenRepository mfaTokenRepository;
    private final UserDeviceFingerprintRepository userDeviceFingerprintRepository;
    private final DeviceFingerprintService deviceFingerprintService;
    private final HttpServletRequest httpServletRequest;
    private final PasetoTokenService pasetoTokenService;
    private final FailedLoginAttemptService failedLoginAttemptService;
    private final SecurityAuditService auditService;
    private final SecurityNotificationService notificationService;
    private final TokenRevocationService tokenRevocationService;

    // Inject maxDevicesPerUser property
    @Value("${security.max-devices-per-user}")
    private int maxDevicesPerUser;

    // --- Constructor ---
    public UserAuthenticator(CacheService cacheService, CaptchaService captchaService,
                             MeterRegistry meterRegistry,
                             UserService userService,
                             UserDeviceFingerprintRepository userDeviceFingerprintRepository,
                             DeviceFingerprintService deviceFingerprintService,
                             HttpServletRequest httpServletRequest,
                             SecurityAuditService auditService,
                             SecurityNotificationService notificationService,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             MfaService mfaService,
                             MfaTokenRepository mfaTokenRepository,
                             PasetoTokenService pasetoTokenService, FailedLoginAttemptService failedLoginAttemptService,
                             TokenRevocationService tokenRevocationService) {
        this.cacheService = cacheService;
        this.captchaService = captchaService;
        this.meterRegistry = meterRegistry;
        this.userService = userService;
        this.userDeviceFingerprintRepository = userDeviceFingerprintRepository;
        this.deviceFingerprintService = deviceFingerprintService;
        this.httpServletRequest = httpServletRequest;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mfaService = mfaService;
        this.mfaTokenRepository = mfaTokenRepository;
        this.pasetoTokenService = pasetoTokenService;
        this.failedLoginAttemptService = failedLoginAttemptService;
        this.tokenRevocationService = tokenRevocationService;
    }

    // --- Registration ---
    @Override
    public AuthResponse register(@Valid UserRegistrationRequest request, String ipAddress) throws TooManyAttemptsException, InvalidCaptchaException, UserAlreadyExistsException, UserRegistrationException {
        try {
            // 0. Pre-flight checks
            performPreFlightChecks(request.getUsername(), ipAddress, "Registration", request.captchaResponse());

            User user = userService.registerUser(request);

            String qrCodeUrl = null;
            if (request.mfaEnabled()) {
                mfaService.enableMfaForUser(user);
                // Fetch token only if needed for QR code URL immediately
                MfaToken mfaToken = mfaTokenRepository.findByUser(user)
                        .orElseThrow(() -> new MfaTokenNotFoundException("MFA Token setup failed during registration"));
                qrCodeUrl = mfaService.generateQrCodeImageUrl(user, mfaToken.getSecret());
            }

            // Generate tokens
            AuthTokenGenerator generateAuthTokens = generateAuthTokens(user);
            auditService.logSuccessfulRegistration(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);

            // --- Associate Fingerprint during Registration ---
            // Generate fingerprint based on registration request details
            String deviceFingerprint = generateAndHandleFingerprint(request.captchaResponse(), null, null); // Use available info, screen might be null
            if (deviceFingerprint != null && user.isDeviceFingerprintingEnabled()) { // Check if enabled
                try {
                    associateFingerprintWithTokens(user, generateAuthTokens.accessToken(), generateAuthTokens.refreshToken(), deviceFingerprint, true); // Mark as trusted on registration
                } catch (MaxDevicesExceededException e) {
                    log.warn("Max devices reached during registration for user [{}]. Fingerprint not associated. Registration continues.", user.getUsername());
                    // POLICY DECISION: Registration currently proceeds even if fingerprint association fails due to device limit.
                    // Consider failing registration here if strict device limits are required.
                    // auditService.logSecurityEvent("MAX_DEVICES_REACHED_ON_REGISTRATION", user.getUserId(), ipAddress);
                }
            }
            // --- End Fingerprint Association ---

            return AuthResponse.builder()
                    .secretImageUrl(qrCodeUrl) // Send QR URL if MFA setup occurred
                    .accessToken(generateAuthTokens.accessToken())
                    .refreshToken(generateAuthTokens.refreshToken())
                    .mfaEnabled(user.isMfaEnabled())
                    .success(true) // Indicate success
                    .message("Registration successful")
                    .build();
        } catch (TooManyAttemptsException | InvalidCaptchaException | UserAlreadyExistsException e) {
            // Re-throw specific exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error during registration for user [{}]: {}", request.username().getUsername(), e.getMessage(), e);
            throw new UserRegistrationException("Registration failed due to an internal error.");
        }
    }

    // --- Authentication ---
    @Override
    public AuthResponse authenticateUser(@Valid LoginRequest request, String ipAddress) throws TooManyAttemptsException, InvalidCaptchaException, AuthenticationFailException {
        try {
            // 0. Pre-flight checks
            validateLoginRequest(request);
            performPreFlightChecks(request.username(), ipAddress, "Login", request.captchaResponse());

            // 1. Fetch user
            User user = userRepository.findByAuthenticationInfoUsername_Username(request.username())
                    .orElseThrow(() -> {
                        auditService.logFailedLogin(request.username(), ipAddress, Constants.USER_NOT_FOUND_MESSAGE);
                        return new UsernameNotFoundException(Constants.INVALID_CREDENTIALS_MESSAGE); // Generic message
                    });

            // 2. Check account lock status
            checkAccountLockStatus(user, ipAddress);

            // 3. Check password
            if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                handleFailedLogin(user, ipAddress); // Increments attempts, locks if needed
                throw new BadCredentialsException(Constants.INVALID_CREDENTIALS_MESSAGE);
            }

            // 4. Handle MFA or proceed to successful login
            if (mfaService.isMfaEnabled(user) || mfaService.isMfaEnforced(user)) {
                // MFA is required
                log.info("MFA required for user [{}]. Initiating challenge.", user.getUsername());
                return initiateMfaChallenge(user);
            } else {
                // No MFA required, proceed to successful login
                log.info("Password validation successful for user [{}]. No MFA required.", user.getUsername());
                String deviceFingerprint = generateAndHandleFingerprint(request.userAgent(), request.screenWidth(), request.screenHeight());
                return handleSuccessfulLogin(user, ipAddress, deviceFingerprint);
            }

        } catch (TooManyAttemptsException | InvalidCaptchaException | BadCredentialsException | LockedException |
                 UsernameNotFoundException e) {
            // Re-throw specific authentication-related exceptions
            throw e;
        } catch (Exception e) {
            // Catch unexpected errors
            log.error("Authentication error for user [{}]: {}", request.username(), e.getMessage(), e);
            throw new AuthenticationFailException("Authentication failed due to an internal error.");
        }
    }

    // --- MFA Verification ---
    @Override
    public AuthResponse verifyMfaAndLogin(MfaVerificationRequest request, String ipAddress) throws TooManyAttemptsException, AuthenticationFailException {
        try {
            // 0. Pre-flight checks (optional for MFA verification itself, but good practice)
            performPreFlightChecks(request.username(), ipAddress, "MFA Verification", request.captchaResponse()); // Captcha likely isn't needed here

            // 1. Fetch user
            User user = userRepository.findByAuthenticationInfoUsername_Username(request.username())
                    .orElseThrow(() -> {
                        auditService.logFailedLogin(request.username(), ipAddress, Constants.USER_NOT_FOUND_MESSAGE);
                        return new UsernameNotFoundException(Constants.INVALID_CREDENTIALS_MESSAGE); // Generic message
                    });

            // 2. Check account lock status (important!)
            checkAccountLockStatus(user, ipAddress);

            // 3. Verify MFA Code
            MfaToken mfaToken = mfaTokenRepository.findByUser(user)
                    .orElseThrow(() -> new MfaException(MfaException.MfaErrorType.SETUP_FAILED, "MFA not set up for user"));

            if (!mfaService.verifyCode(mfaToken.getSecret(), request.mfaCode())) {
                auditService.logMfaVerificationFailed(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);
                // TODO: POLICY DECISION: Should failed MFA attempts lock the account?
                // If yes, uncomment the line below. Be cautious about denial-of-service.
                // handleFailedLogin(user, ipAddress); // Optionally lock after too many MFA failures
                throw new MfaVerificationFailedException(Constants.INVALID_MFA_CODE_MESSAGE);
            }

            // 4. MFA successful, proceed to login completion
            log.info("MFA verification successful for user [{}]", user.getUsername());
            String deviceFingerprint = generateAndHandleFingerprint(request.userAgent(), request.screenWidth(), request.screenHeight());
            return handleSuccessfulLogin(user, ipAddress, deviceFingerprint);

        } catch (TooManyAttemptsException | LockedException | UsernameNotFoundException | MfaException |
                 MfaVerificationFailedException e) {
            // Re-throw specific exceptions
            throw e;
        } catch (Exception e) {
            log.error("MFA Verification error for user [{}]: {}", request.username(), e.getMessage(), e);
            throw new AuthenticationFailException("MFA verification failed due to an internal error.");
        }
    }

    // --- Logout ---
    @Transactional
    @Override
    public void logout(String accessToken, String refreshToken) { // Accept tokens to revoke
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"))) {
            String username = authentication.getName();
            String userId = ""; // Try to get user ID if principal is User object
            if (authentication.getPrincipal() instanceof User userPrincipal) {
                userId = String.valueOf(userPrincipal.getUserId());
            }
            log.info("Logging out user: {}", username);

            String reason = "User initiated logout"; // Define a reason for logging

            // Standard Spring Security logout (clears context, invalidates session if applicable)
            new SecurityContextLogoutHandler().logout(httpServletRequest, null, authentication);

            // Revoke tokens by adding them to the blocklist
            if (StringUtils.isNotBlank(accessToken)) {
                tokenRevocationService.revokeAccessToken(accessToken, TokenConstants.ACCESS_TOKEN_DURATION, reason); // Use appropriate duration
            }
            if (StringUtils.isNotBlank(refreshToken)) {
                tokenRevocationService.revokeRefreshToken(refreshToken, TokenConstants.REFRESH_TOKEN_DURATION, reason); // Use appropriate duration
            }
            auditService.logLogout(userId, username, httpServletRequest.getRemoteAddr()); // Pass more info to audit
        } else {
            log.debug("Logout called but no authenticated user found in context.");
        }
        // No return value needed typically
    }

    // --- Helper Methods ---

    private void validateLoginRequest(LoginRequest loginRequest) {
        if (loginRequest == null ||
                StringUtils.isBlank(loginRequest.username()) ||
                StringUtils.isBlank(loginRequest.password())) {
            throw new IllegalArgumentException("Username and password cannot be empty.");
        }
        // TODO: Add captcha validation here or in preFlightChecks if required for login
        // if (!captchaService.validateCaptcha(loginRequest.captchaResponse(), ipAddress)) {
        //     throw new InvalidCaptchaException("Invalid CAPTCHA response");
        // }
    }

    /**
     * Consolidated pre-flight checks for registration and login.
     */
    private void performPreFlightChecks(String username, String ipAddress, String actionType, String captchaResponse) throws TooManyAttemptsException {

        // Check failed login attempts (applies to log in/MFA verify, maybe registration too)
        try {
            failedLoginAttemptService.checkAndThrowIfBlocked(username); // Check by username
            failedLoginAttemptService.checkAndThrowIfBlocked(ipAddress); // Check by IP
        } catch (TooManyAttemptsException e) {
            // Use the exception message which likely contains the key type information
            auditService.logFailedLogin(username, ipAddress, "Too Many Attempts (Backoff) for " + e.getMessage());
            throw e;
        }

        // TODO: Add Captcha validation here if required for the actionType
        if ("Login".equals(actionType) || "Registration".equals(actionType)) {
            captchaService.verifyRateLimitAndCaptcha(captchaResponse, ipAddress);
        }

        log.trace("Pre-flight checks passed for user [{}], action [{}]", username, actionType);
    }

    private void checkAccountLockStatus(User user, String ipAddress) throws LockedException {
        if (!user.isAccountNonLocked()) {
            if (!unlockWhenTimeExpired(user)) {
                // The Account is still locked
                auditService.logAccessDeniedLockedAccount(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);
                throw new LockedException(Constants.ACCOUNT_LOCKED_MESSAGE);
            } else {
                // The Account was locked but now unlocked by time expiry
                auditService.logAccountUnlocked(String.valueOf(user.getUserId()), user.getUsername(), ipAddress + " (Automatic)");
                log.info("Account for user [{}] automatically unlocked due to time expiry.", user.getUsername());
            }
        }
    }

    @Override
    public MfaToken initializeMfaToken(User user) {
        String secret = mfaService.generateSecretKey();
        MfaToken newToken = new MfaToken(user, secret, mfaService.isMfaEnforced(user));
        return mfaTokenRepository.save(newToken);
    }

    @Override
    public AuthResponse initiateMfaChallenge(User user) {
        try {
            MfaToken mfaToken = mfaTokenRepository.findByUser(user)
                    .orElseGet(() -> initializeMfaToken(user)); // Ensure token exists

            Map<String, Object> details = new HashMap<>();
            details.put("mfaRequired", true);
            String message = "MFA verification required.";

            // Check if the token is enabled (meaning setup is complete)
            if (!mfaToken.isEnabled()) {
                // If MFA needs setup, provide QR code details
                String qrCodeUrl = mfaService.generateQrCodeImageUrl(user, mfaToken.getSecret());
                details.put("secret", mfaToken.getSecret()); // Send secret only during setup
                details.put("qrCodeUrl", qrCodeUrl);
                details.put("setupRequired", true);
                message = "MFA setup required.";
                log.info("Initiating MFA setup challenge for user: {}", user.getUsername());
                auditService.logMfaChallengeInitiated(String.valueOf(user.getUserId()), user.getUsername(), httpServletRequest.getRemoteAddr());
            } else {
                log.info("Initiating MFA verification challenge for user: {}", user.getUsername());
                details.put("setupRequired", false); // Explicitly state setup is not required
                auditService.logMfaChallengeInitiated(String.valueOf(user.getUserId()), user.getUsername(), httpServletRequest.getRemoteAddr());
            }

            // Return an AuthResponse indicating MFA is needed, optionally with setup details
            return AuthResponse.builder()
                    .success(false) // Not fully logged in yet
                    .mfaRequired(true)
                    .mfaEnabled(mfaToken.isEnabled()) // Reflects if setup is done
                    .secretImageUrl((String) details.get("qrCodeUrl")) // Null if setup not required
                    .message(message)
                    .details(details) // Include details map
                    .build();
        } catch (Exception e) {
            log.error("Error initiating MFA challenge for user: {}", user.getUsername(), e);
            throw new MfaException(MfaException.MfaErrorType.CHALLENGE_FAILED, "Failed to initiate MFA challenge");
        }
    }

    @Override
    @Transactional // Ensure all operations succeed or fail together
    public AuthResponse handleSuccessfulLogin(User user, String ipAddress, String deviceFingerprint) {
        resetFailedAttempts(user); // Resets attempts in DB and cache
        user.updateLastLoginTime(Instant.now());
        userRepository.save(user); // Save updated last login time

        auditService.logSuccessfulLogin(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);

        // Generate tokens
        AuthTokenGenerator tokens = generateAuthTokens(user);

        // Associate fingerprint with tokens if enabled and fingerprint available
        if (user.isDeviceFingerprintingEnabled() && deviceFingerprint != null) {
            try {
                // Associate, marking as untrusted if it's a new device found during login
                associateFingerprintWithTokens(user, tokens.accessToken(), tokens.refreshToken(), deviceFingerprint, false); // false = not trusted by default on login
            } catch (MaxDevicesExceededException e) {
                log.warn("Max devices reached during login for user [{}]. Fingerprint not associated. Login continues.", user.getUsername());
                // POLICY DECISION: Login currently proceeds even if fingerprint association fails due to device limit.
                // Consider failing login here if strict device limits are required:
                // throw new AuthenticationFailException("Cannot log in, maximum device limit reached.");
                // auditService.logSecurityEvent("MAX_DEVICES_REACHED_ON_LOGIN", user.getUserId(), ipAddress);
            }
        } else if (deviceFingerprint == null && user.isDeviceFingerprintingEnabled()) {
            log.warn("Device fingerprint could not be generated during login for user [{}], but fingerprinting is enabled. Proceeding without association.", user.getUsername());
        }

        return AuthResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .username(user.getUsername())
                .authorities(user.getAuthorities()) // Assuming getAuthorities returns Collection<? extends GrantedAuthority>
                .success(true)
                .message("Login successful")
                .mfaEnabled(user.isMfaEnabled()) // Include MFA status
                .build();
    }

    @Override
    @Transactional
    public void handleFailedLogin(User user, String ipAddress) {
        increaseFailedAttempts(user); // This saves the user and records in cache
        auditService.logFailedLogin(user.getUsername(), ipAddress, Constants.INVALID_CREDENTIALS_MESSAGE);

        // Check lock condition using the updated count from the user entity
        if (user.getFailedLoginAttempts() >= Constants.MAX_FAILED_ATTEMPTS) {
            lockUser(user); // This saves the user
            notificationService.sendAccountLockedEmail(user);
            auditService.logAccountLocked(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);
        }
    }

    /**
     * @deprecated Redundant method. Logic is handled by {@link #checkAccountLockStatus(User, String)}.
     */
    @Deprecated(since = "2025-04-20", forRemoval = true) // Mark as deprecated
//    @Override
    public void handleLockedAccount(User user, String ipAddress) throws LockedException {
        // Implementation removed as logic is in checkAccountLockStatus
        log.warn("handleLockedAccount method is deprecated and should not be used directly. Use checkAccountLockStatus.");
        // checkAccountLockStatus(user, ipAddress); // No need to call it here, the caller should use checkAccountLockStatus directly
    }

    /**
     * Associates device fingerprint with tokens, checking device limits for new devices.
     *
     * @param user              The user.
     * @param accessToken       The access token.
     * @param refreshToken      The refresh token.
     * @param deviceFingerprint The generated device fingerprint.
     * @param trustOnCreate     Whether to mark the device as trusted if it's being created (e.g., during registration).
     * @throws MaxDevicesExceededException if adding a new device would exceed the limit.
     */
    private void associateFingerprintWithTokens(User user, String accessToken, String refreshToken, String deviceFingerprint, boolean trustOnCreate) throws MaxDevicesExceededException {
        Optional<UserDeviceFingerprint> existingOpt = userDeviceFingerprintRepository
                .findByUserAndDeviceFingerprint(user, deviceFingerprint);

        UserDeviceFingerprint userDeviceFingerprint;
        boolean isNewDevice = existingOpt.isEmpty();

        if (isNewDevice) {
            // --- New Device ---
            log.debug("Associating new device fingerprint for user [{}]", user.getUsername());
            // Check device limit BEFORE creating
            long deviceCount = userDeviceFingerprintRepository.countByUserAndActiveTrue(user);
            if (deviceCount >= maxDevicesPerUser) {
                log.warn("Max device limit ({}) reached for user [{}]. Cannot associate new device fingerprint.", maxDevicesPerUser, user.getUsername());
                throw new MaxDevicesExceededException("Maximum device limit reached, cannot add new device.");
            }
            userDeviceFingerprint = UserDeviceFingerprint.builder()
                    .user(user)
                    .deviceFingerprint(deviceFingerprint)
                    .isActive(true)
                    .trusted(trustOnCreate) // Set trust status based on context (e.g., true for registration, false for login)
                    .failedAttempts(0)
                    // TODO: Derive deviceName/browserInfo if possible from request/fingerprint data
                    // .deviceName("Derived Name")
                    // .browserInfo("Derived Info")
                    .build();
        } else {
            // --- Existing Device ---
            log.debug("Updating existing device fingerprint association for user [{}]", user.getUsername());
            userDeviceFingerprint = existingOpt.get();
            userDeviceFingerprint.setActive(true); // Ensure it's active if used successfully
            userDeviceFingerprint.setDeactivatedAt(null); // Clear deactivation time if re-activated
            userDeviceFingerprint.setFailedAttempts(0); // Reset failed attempts on successful association
        }

        // Update common fields
        userDeviceFingerprint.setAccessToken(accessToken); // Store/Update the access token
        userDeviceFingerprint.setRefreshToken(refreshToken); // Store/Update the refresh token
        userDeviceFingerprint.setLastUsedAt(Instant.now());
        userDeviceFingerprint.setLastKnownIp(httpServletRequest.getRemoteAddr()); // Update IP

        userDeviceFingerprintRepository.save(userDeviceFingerprint);
        log.info("Successfully associated tokens with {} fingerprint for user [{}] (Trusted: {})",
                isNewDevice ? "new" : "existing", user.getUsername(), userDeviceFingerprint.isTrusted());
    }


    @Override
    @Transactional
    public void resetFailedAttempts(User user) {
        String username = user.getUsername();
        // Check if there's anything to reset to avoid unnecessary DB write/cache op
        if (user.getFailedLoginAttempts() > 0 || user.getLockTime() != null) {
            failedLoginAttemptService.resetAttempts(username); // Reset cache/counter
            user.updateFailedLoginAttempts(0);
            user.updateLockTime(null);
            // Save user to persist changes to embedded AccountStatus
            userRepository.save(user);
            log.debug("Reset failed login attempts for user [{}]", user.getUsername());
        } else {
            log.trace("No failed attempts or lock time to reset for user [{}]", user.getUsername());
        }
    }

    @Override
    @Transactional
    public void increaseFailedAttempts(User user) {
        int newAttemptCount = user.getFailedLoginAttempts() + 1;
        user.updateFailedLoginAttempts(newAttemptCount);
        userRepository.save(user); // Save updated count
        failedLoginAttemptService.recordFailure(user.getUsername()); // Record in cache/counter
        log.debug("Increased failed login attempts for user [{}] to {}", user.getUsername(), newAttemptCount);
    }

    @Override
    @Transactional
    public void lockUser(User user) {
        if (user.isAccountNonLocked()) { // Only lock if not already locked
            user.updateAccountNonLocked(false);
            user.updateLockTime(Instant.now());
            userRepository.save(user);
            log.warn("Locked account for user [{}] due to excessive failed attempts.", user.getUsername());
        } else {
            log.debug("Account for user [{}] is already locked.", user.getUsername());
        }
    }

    @Override
    @Transactional
    public void unlockUser(String username) {
        User user = userRepository.findByAuthenticationInfoUsername_Username(username)
                .orElseThrow(() -> new UsernameNotFoundException(Constants.USER_NOT_FOUND_MESSAGE));
        if (!user.isAccountNonLocked()) {
            user.updateAccountNonLocked(true);
            user.updateLockTime(null);
            user.updateFailedLoginAttempts(0); // Reset attempts on manual unlock
            userRepository.save(user);
            failedLoginAttemptService.resetAttempts(username); // Also reset cache/counter
            log.info("Manually unlocked account for user [{}]", username);
            auditService.logAccountUnlocked(String.valueOf(user.getUserId()), username, "Manual Unlock"); // Add context
        } else {
            log.debug("Account for user [{}] is already unlocked.", username);
        }
    }

    @Override
    @Transactional
    public boolean unlockWhenTimeExpired(User user) {
        // Check if actually locked and lock time is set
        if (!user.isAccountNonLocked() && user.getLockTime() != null) {
            long lockTimeInMillis = user.getLockTime().toEpochMilli();
            long currentTimeInMillis = System.currentTimeMillis();

            if (currentTimeInMillis - lockTimeInMillis >= Constants.LOCK_DURATION_MILLIS) {
                user.updateAccountNonLocked(true);
                user.updateLockTime(null);
                user.updateFailedLoginAttempts(0); // Reset attempts on auto-unlock
                userRepository.save(user);
                failedLoginAttemptService.resetAttempts(user.getUsername()); // Reset cache/counter
                log.info("Account for user [{}] automatically unlocked.", user.getUsername());
                // Audit log is handled in checkAccountLockStatus
                return true; // Unlocked now
            }
            return false; // Still locked
        }
        return true; // Not locked or no lock time set
    }

    @Override
    public String generateAndHandleFingerprint(String userAgent, String screenWidth, String screenHeight) {
        String deviceFingerprint = deviceFingerprintService.generateDeviceFingerprint(userAgent, screenWidth, screenHeight, httpServletRequest);

        if (deviceFingerprint == null) {
            log.warn("Device fingerprint could not be generated.");
            // Policy decision: Allow login without fingerprint? For now, return null.
            // If fingerprinting is mandatory, could throw an exception here.
            // throw new DeviceFingerprintGenerationException("Could not generate mandatory device fingerprint");
            return null;
        }
        log.trace("Generated device fingerprint: {}", deviceFingerprint); // Log trace level
        return deviceFingerprint;
    }

    @Override
    public String determineUserScope() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty()) {
            // Handle anonymous or unauthenticated users appropriately
            return "ROLE_ANONYMOUS"; // Or throw exception if authentication is strictly required here
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::isNotBlank) // Ensure authorities are not blank
                .collect(Collectors.joining(" "));
    }

    // --- Token Generation ---
    @NotNull
    private AuthTokenGenerator generateAuthTokens(User user) {
        String userId = String.valueOf(user.getUserId());
        // Consider adding roles/authorities to access token claims if needed, but keep it minimal
        var accessToken = pasetoTokenService.generatePublicAccessToken(userId, TokenConstants.ACCESS_TOKEN_DURATION);
        // Refresh token typically only needs user ID for lookup during refresh
        var refreshToken = pasetoTokenService.generateRefreshToken(user); // Assuming this generates a suitable refresh token
        log.debug("Generated new tokens for user ID [{}]", userId);
        return new AuthTokenGenerator(accessToken, refreshToken);
    }

    private record AuthTokenGenerator(String accessToken, String refreshToken) {
    }

    // --- Duration Getters ---
    @Override
    public Duration getAccessTokenDuration() {
        return TokenConstants.ACCESS_TOKEN_DURATION;
    }

    @Override
    public Duration getRefreshTokenDuration() {
        return TokenConstants.REFRESH_TOKEN_DURATION;
    }

    @Override
    public Duration getJtiDuration() {
        return TokenConstants.JTI_DURATION;
    }

    // --- Authentication Info Caching ---
    @Transactional(readOnly = true)
    @Override
    public AuthenticationInfo getAuthenticationInfo(String username) {
        io.micrometer.core.instrument.Timer.Sample sample = Timer.start(meterRegistry);
        String cacheKey = AUTH_CACHE_KEY_PREFIX + username;
        try {
            // Use cacheService which handles cache misses and supplier execution
            AuthenticationInfo info = cacheService.getOrCache(
                    AUTH_CACHE,
                    cacheKey,
                    () -> userRepository.findByAuthenticationInfoUsername_Username(username)
                            .orElseThrow(() -> new UsernameNotFoundException("AuthInfo not found for user: " + username)) // Handle not found in supplier
            ).getAuthenticationInfo();
            sample.stop(meterRegistry.timer("auth.info.fetch", "status", "success"));
            return info;
        } catch (Exception e) {
            sample.stop(meterRegistry.timer("auth.info.fetch", "status", "error"));
            log.error("Failed to fetch authentication info for user [{}]: {}", username, e.getMessage(), e);
            // Re-throw specific exceptions if needed, or a generic one
            if (e instanceof UsernameNotFoundException) {
                throw e;
            }
            throw new RuntimeException("Failed to fetch authentication info", e); // Or custom exception
        }
    }

    @Transactional(noRollbackFor = Exception.class) // Allow commit even if cache eviction fails? Review this.
    // Corrected CacheEvict key to use the constant properly with SpEL
    @CacheEvict(value = AUTH_CACHE, key = "'" + Constants.AUTH_CACHE_KEY_PREFIX + "' + #username")
    @Override
    public void updateAuthenticationInfo(String username, AuthenticationInfo newInfo) {
        if (username == null || newInfo == null) {
            throw new IllegalArgumentException("Username and new info cannot be null");
        }
        if (!username.equals(newInfo.getUsername())) {
            throw new IllegalArgumentException("Username mismatch in updateAuthenticationInfo");
        }

        // Fetch existing user to ensure they exist before updating auth info
        User user = userRepository.findByAuthenticationInfoUsername_Username(username)
                .orElseThrow(() -> new UsernameNotFoundException("Cannot update auth info, user not found: " + username));

        // Update the embedded object on the user entity
        user.updateAuthentication(newInfo); // Assuming the User entity has a method to update the embedded object
        userRepository.save(user); // Save the user entity with the updated embedded object

        // Log success - Cache eviction is handled by @CacheEvict
        log.info("Updated authentication info for user [{}] and evicted cache entry", username);

        // Removed manual cache invalidation - handled by @CacheEvict
        // cacheService.invalidate(AUTH_CACHE, AUTH_CACHE_KEY_PREFIX + username);
    }

    // --- REMOVED Unused/Deprecated Methods ---
    // private Authentication performAuthentication(LoginRequest loginRequest) { ... }
    // public Authentication createSuccessfulAuthentication(User user) { ... }
    // public void handleLockedAccount(User user, String ipAddress) { ... } // Removed as deprecated
}


// TODO: Consider backup codes for account recovery
// TODO: Review transaction boundaries and propagation, especially around cache operations.
// TODO: Add more specific metrics for fingerprinting success/failure/new device detection.