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
import me.amlu.shop.amlume_shop.resilience.service.ResilienceService;
import me.amlu.shop.amlume_shop.security.failedlogin.FailedLoginAttemptService;
import me.amlu.shop.amlume_shop.security.paseto.PasetoTokenService;
import me.amlu.shop.amlume_shop.security.paseto.TokenRevocationService;
import me.amlu.shop.amlume_shop.service.CacheService;
import me.amlu.shop.amlume_shop.user_management.AuthenticationInfo;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.user_management.UserRepository;
import me.amlu.shop.amlume_shop.user_management.UserService;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
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
import java.util.stream.Collectors;

import static me.amlu.shop.amlume_shop.commons.Constants.AUTH_CACHE;
import static me.amlu.shop.amlume_shop.commons.Constants.AUTH_CACHE_KEY_PREFIX;

@Service
@Transactional
public class UserAuthenticator implements AuthenticationInterface {

    // --- Constants ---
    private static final int ACCESS_TOKEN_DURATION_INT = 15;
    private static final String ACCOUNT_LOCKED_MESSAGE = "Account is locked";
    private static final String USER_NOT_FOUND_MESSAGE = "User not found";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password"; // More generic
    private static final String INVALID_MFA_CODE_MESSAGE = "Invalid MFA code";
    private static final Duration ACCESS_TOKEN_DURATION = Duration.ofMinutes(ACCESS_TOKEN_DURATION_INT);
    private static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(7);
    private static final Duration JTI_DURATION = Duration.ofMinutes(ACCESS_TOKEN_DURATION_INT + (long) 1); // Used for blocklist TTL
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UserAuthenticator.class);

    private final CacheService cacheService;
    private final MeterRegistry meterRegistry;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MfaService mfaService;
    private final MfaTokenRepository mfaTokenRepository;
    private final UserDeviceFingerprintRepository userDeviceFingerprintRepository;
    private final DeviceFingerprintService deviceFingerprintService;
    private final HttpServletRequest httpServletRequest;
    private final ResilienceService resilienceService;
    private final PasetoTokenService pasetoTokenService;
    private final FailedLoginAttemptService failedLoginAttemptService;
    private final CaptchaService captchaService;
    private final SecurityAuditService auditService;
    private final SecurityNotificationService notificationService;
    private final TokenRevocationService tokenRevocationService; // Inject a service to handle blocklisting

    // --- Constructor ---
    public UserAuthenticator(CacheService cacheService,
                             MeterRegistry meterRegistry,
                             UserService userService,
                             UserDeviceFingerprintRepository userDeviceFingerprintRepository,
                             DeviceFingerprintService deviceFingerprintService,
                             HttpServletRequest httpServletRequest,
                             ResilienceService resilienceService,
                             CaptchaService captchaService,
                             SecurityAuditService auditService,
                             SecurityNotificationService notificationService,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             MfaService mfaService,
                             MfaTokenRepository mfaTokenRepository,
                             PasetoTokenService pasetoTokenService, FailedLoginAttemptService failedLoginAttemptService,
                             TokenRevocationService tokenRevocationService) {
        this.cacheService = cacheService;
        this.meterRegistry = meterRegistry;
        this.userService = userService;
        this.userDeviceFingerprintRepository = userDeviceFingerprintRepository;
        this.deviceFingerprintService = deviceFingerprintService;
        this.httpServletRequest = httpServletRequest;
        this.resilienceService = resilienceService;
        this.captchaService = captchaService;
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
            // 0. Pre-flight checks, resilience
            performPreFlightChecks(request.username().getUsername(), request.captchaResponse(), ipAddress, "Registration");

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
            // 0. Pre-flight checks, resilience
            validateLoginRequest(request);
            performPreFlightChecks(request.getUsername(), request.getCaptchaResponse(), ipAddress, "Login");

            // 1. Fetch user
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> {
                        auditService.logFailedLogin(request.getUsername(), ipAddress, USER_NOT_FOUND_MESSAGE);
                        return new UsernameNotFoundException(INVALID_CREDENTIALS_MESSAGE); // Generic message
                    });

            // 2. Check account lock status
            checkAccountLockStatus(user, ipAddress);

            // 3. Check password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                handleFailedLogin(user, ipAddress); // Increments attempts, locks if needed
                throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
            }

            // 4. Handle MFA or proceed to successful login
            if (mfaService.isMfaEnabled(user) || mfaService.isMfaEnforced(user)) {
                // MFA is required, but no code provided yet -> Initiate challenge
                // This path assumes the initial login request doesn't include the MFA code.
                // If it could, you'd need to check request.getMfaCode() here.
                return initiateMfaChallenge(user);
            } else {
                // No MFA required, proceed to successful login
                String deviceFingerprint = generateAndHandleFingerprint(request.getUserAgent(), request.getScreenWidth(), request.getScreenHeight());
                return handleSuccessfulLogin(user, ipAddress, deviceFingerprint);
            }

        } catch (TooManyAttemptsException | InvalidCaptchaException | BadCredentialsException | LockedException |
                 UsernameNotFoundException e) {
            // Re-throw specific authentication-related exceptions
            throw e;
        } catch (Exception e) {
            // Catch unexpected errors
            log.error("Authentication error for user [{}]: {}", request.getUsername(), e.getMessage(), e);
            throw new AuthenticationFailException("Authentication failed due to an internal error.");
        }
    }

    // --- MFA Verification ---
    @Override
    public AuthResponse verifyMfaAndLogin(MfaVerificationRequest request, String ipAddress) throws TooManyAttemptsException, AuthenticationFailException {
        try {
            // 0. Pre-flight checks (optional for MFA verification itself, but good practice)
            performPreFlightChecks(request.getUsername(), null, ipAddress, "MFA Verification"); // Captcha likely not needed here

            // 1. Fetch user
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> {
                        auditService.logFailedLogin(request.getUsername(), ipAddress, USER_NOT_FOUND_MESSAGE);
                        return new UsernameNotFoundException(INVALID_CREDENTIALS_MESSAGE); // Generic message
                    });

            // 2. Check account lock status (important!)
            checkAccountLockStatus(user, ipAddress);

            // 3. Verify MFA Code
            MfaToken mfaToken = mfaTokenRepository.findByUser(user)
                    .orElseThrow(() -> new MfaException(MfaException.MfaErrorType.SETUP_FAILED, "MFA not set up for user"));

            if (!mfaService.verifyCode(mfaToken.getSecret(), request.getMfaCode())) {
                auditService.logMfaVerificationFailed(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);
                // Consider incrementing failed attempts here too? Depends on security policy.
                // handleFailedLogin(user, ipAddress); // Optionally lock after too many MFA failures
                throw new MfaVerificationFailedException(INVALID_MFA_CODE_MESSAGE);
            }

            // 4. MFA successful, proceed to login completion
            String deviceFingerprint = generateAndHandleFingerprint(request.getUserAgent(), request.getScreenWidth(), request.getScreenHeight());
            return handleSuccessfulLogin(user, ipAddress, deviceFingerprint);

        } catch (TooManyAttemptsException | LockedException | UsernameNotFoundException | MfaException |
                 MfaVerificationFailedException e) {
            // Re-throw specific exceptions
            throw e;
        } catch (Exception e) {
            log.error("MFA Verification error for user [{}]: {}", request.getUsername(), e.getMessage(), e);
            throw new AuthenticationFailException("MFA verification failed due to an internal error.");
        }
    }


    // --- Logout ---
    @Transactional
    @Override
    public void logout(String accessToken, String refreshToken) { // Accept tokens to revoke
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String username = authentication.getName();
            log.info("Logging out user: {}", username);

            String reason = "Manually User logged out"; // Define a reason for logging

            // Standard Spring Security logout
            new SecurityContextLogoutHandler().logout(httpServletRequest, null, authentication);

            // Revoke tokens by adding them to the blocklist
            if (StringUtils.isNotBlank(accessToken)) {
                tokenRevocationService.revokeAccessToken(accessToken, ACCESS_TOKEN_DURATION, reason); // Use appropriate duration
            }
            if (StringUtils.isNotBlank(refreshToken)) {
                tokenRevocationService.revokeRefreshToken(refreshToken, REFRESH_TOKEN_DURATION, reason); // Use appropriate duration
            }
            auditService.logLogout(username, reason); // Assuming username is principal name
        } else {
            log.debug("Logout called but no authentication found in context.");
        }
        // No return value needed typically
    }

    // --- Helper Methods ---

    private void validateLoginRequest(LoginRequest loginRequest) {
        if (loginRequest == null ||
                StringUtils.isBlank(loginRequest.getUsername()) ||
                StringUtils.isBlank(loginRequest.getPassword())) {
            throw new IllegalArgumentException("Username and password cannot be empty.");
        }
    }

    /**
     * Consolidated pre-flight checks for registration and login.
     */
    private void performPreFlightChecks(String username, String captchaResponse, String ipAddress, String actionType) throws TooManyAttemptsException, InvalidCaptchaException {
        // Check IP rate limiting
        if (!resilienceService.allowRequestByIp(ipAddress)) {
            auditService.logFailedLogin(username, ipAddress, "IP Rate Limit Exceeded");
            throw new TooManyAttemptsException("Too many requests from this IP. Please try again later.");
        }

        // Check Username rate limiting (if username is provided)
        if (StringUtils.isNotBlank(username) && !resilienceService.allowRequestByUsername(username)) {
            auditService.logFailedLogin(username, ipAddress, "Username Rate Limit Exceeded");
            throw new TooManyAttemptsException("Too many requests for this user. Please try again later.");
        }

        // IMPLEMENT if necessary. Check if we need to wait (exponential backoff)

        // Check failed login attempts
        try {
            failedLoginAttemptService.checkAndThrowIfBlocked(username);
        } catch (TooManyAttemptsException e) {
            auditService.logFailedLogin(username, ipAddress, "Too Many Login Attempts (Backoff)");
            throw e;
        }

        // Validate CAPTCHA if required (e.g., based on attempt count or always for registration)
        // Add logic here if captcha requirement is dynamic
        try {
            captchaService.verifyRateLimitAndCaptcha(ipAddress, captchaResponse);
        } catch (InvalidCaptchaException e) {
            auditService.logFailedLogin(username, ipAddress, "Invalid CAPTCHA");
            throw new InvalidCaptchaException("Invalid CAPTCHA");
        }

        log.trace("Pre-flight checks passed for user [{}], action [{}]", username, actionType);
    }

    private void checkAccountLockStatus(User user, String ipAddress) throws LockedException {
        if (!user.isAccountNonLocked()) {
            if (!unlockWhenTimeExpired(user)) {
                // Account is still locked
                auditService.logAccessDeniedLockedAccount(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);
                throw new LockedException(ACCOUNT_LOCKED_MESSAGE);
            } else {
                // Account was locked but now unlocked by time expiry
                auditService.logAccountUnlocked(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);
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

            if (!mfaToken.isEnabled()) {
                // If MFA needs setup, provide QR code details
                String qrCodeUrl = mfaService.generateQrCodeImageUrl(user, mfaToken.getSecret());
                details.put("secret", mfaToken.getSecret()); // Send secret only during setup
                details.put("qrCodeUrl", qrCodeUrl);
                details.put("setupRequired", true);
                message = "MFA setup required.";
            }

            // Return an AuthResponse indicating MFA is needed, optionally with setup details
            return AuthResponse.builder()
                    .success(false) // Not fully logged in yet
                    .mfaRequired(true)
                    .mfaSetupRequired(details.containsKey("setupRequired"))
                    .secretImageUrl((String) details.get("qrCodeUrl"))
                    .message(message)
                    .build();
        } catch (Exception e) {
            log.error("Error initiating MFA challenge for user: {}", user.getUsername(), e);
            throw new MfaException(MfaException.MfaErrorType.CHALLENGE_FAILED, "Failed to initiate MFA challenge");
        }
    }

    @Override
    @Transactional // Ensure all operations succeed or fail together
    public AuthResponse handleSuccessfulLogin(User user, String ipAddress, String deviceFingerprint) {
        resetFailedAttempts(user);
        user.setLastLoginTime(Instant.now());
        // No need to save user here if resetFailedAttempts already saves

        auditService.logSuccessfulLogin(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);

        // Generate tokens
        AuthTokenGenerator tokens = generateAuthTokens(user);

        // Associate fingerprint with tokens
        if (deviceFingerprint != null) {
            associateFingerprintWithTokens(user, tokens.accessToken(), tokens.refreshToken(), deviceFingerprint);
            // REMOVED: deviceFingerprintService.storeOrUpdateFingerprint(user, accessToken, refreshToken, deviceFingerprint);
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
        increaseFailedAttempts(user); // This saves the user
        auditService.logFailedLogin(user.getUsername(), ipAddress, INVALID_CREDENTIALS_MESSAGE);

        if (user.getFailedLoginAttempts() >= Constants.MAX_FAILED_ATTEMPTS) {
            lockUser(user); // This saves the user
            notificationService.sendAccountLockedEmail(user);
            auditService.logAccountLocked(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);
        }
    }

    @Override
    public void handleLockedAccount(User user, String ipAddress) throws LockedException {
        // This method seems redundant now as checkAccountLockStatus handles the logic
        // Kept for interface compliance, but logic is in checkAccountLockStatus
        checkAccountLockStatus(user, ipAddress);
    }

    private void associateFingerprintWithTokens(User user, String accessToken, String refreshToken, String deviceFingerprint) {
        // Consider finding existing fingerprint record for this user/fingerprint combo first
        // to update tokens/lastUsedAt instead of always creating new ones.
        UserDeviceFingerprint userDeviceFingerprint = userDeviceFingerprintRepository
                .findByUserAndDeviceFingerprint(user, deviceFingerprint)
                .orElse(new UserDeviceFingerprint()); // Create new if not found

        userDeviceFingerprint.setUser(user);
        userDeviceFingerprint.setDeviceFingerprint(deviceFingerprint);
        userDeviceFingerprint.setAccessToken(accessToken); // Store the access token
        userDeviceFingerprint.setRefreshToken(refreshToken); // Store the refresh token
        userDeviceFingerprint.setLastUsedAt(Instant.now());
        userDeviceFingerprintRepository.save(userDeviceFingerprint);
        log.debug("Associated tokens with fingerprint for user [{}]", user.getUsername());
    }

    @Override
    @Transactional
    public void resetFailedAttempts(User user) {
        String username = user.getUsername();
        if (user.getFailedLoginAttempts() > 0 || user.getLockTime() != null) {
            failedLoginAttemptService.resetAttempts(username);
            user.setFailedLoginAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);
            log.debug("Reset failed login attempts for user [{}]", user.getUsername());
        }
    }

    @Override
    @Transactional
    public void increaseFailedAttempts(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        userRepository.save(user);
        failedLoginAttemptService.recordFailure(user.getUsername());
        log.debug("Increased failed login attempts for user [{}] to {}", user.getUsername(), user.getFailedLoginAttempts());
    }

    @Override
    @Transactional
    public void lockUser(User user) {
        user.setAccountNonLocked(false);
        user.setLockTime(Instant.now());
        userRepository.save(user);
        log.warn("Locked account for user [{}] due to excessive failed attempts.", user.getUsername());
    }

    @Override
    @Transactional
    public void unlockUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE));
        if (!user.isAccountNonLocked()) {
            user.setAccountNonLocked(true);
            user.setLockTime(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            log.info("Manually unlocked account for user [{}]", username);
            auditService.logAccountUnlocked(String.valueOf(user.getUserId()), username, "Manual Unlock"); // Add context
        } else {
            log.debug("Account for user [{}] is already unlocked.", username);
        }
    }

    @Override
    @Transactional
    public boolean unlockWhenTimeExpired(User user) {
        if (user.getLockTime() != null && !user.isAccountNonLocked()) { // Check if actually locked
            long lockTimeInMillis = user.getLockTime().toEpochMilli();
            long currentTimeInMillis = System.currentTimeMillis();

            if (currentTimeInMillis - lockTimeInMillis >= Constants.LOCK_DURATION_MILLIS) {
                user.setAccountNonLocked(true);
                user.setLockTime(null);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
                log.info("Account for user [{}] automatically unlocked.", user.getUsername());
                return true; // Unlocked now
            }
            return false; // Still locked
        }
        return true; // Not locked in the first place
    }

    @Override
    public String generateAndHandleFingerprint(String userAgent, String screenWidth, String screenHeight) {
        String deviceFingerprint = deviceFingerprintService.generateDeviceFingerprint(userAgent, screenWidth, screenHeight, httpServletRequest);

        if (deviceFingerprint == null) {
            log.warn("Device fingerprint could not be generated.");
            // Decide policy: Allow login without fingerprint? For now, return null.
            return null;
        }
        return deviceFingerprint;
    }

    @Override
    public String determineUserScope() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getAuthorities().isEmpty()) {
            // Handle anonymous or unauthenticated users appropriately
            return "ROLE_ANONYMOUS"; // Or throw exception if authentication is strictly required here
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));
    }

    // --- Token Generation ---
    @NotNull
    private AuthTokenGenerator generateAuthTokens(User user) {
        String userId = String.valueOf(user.getUserId());
        // Consider adding roles/authorities to access token claims if needed, but keep it minimal
        var accessToken = pasetoTokenService.generatePublicAccessToken(userId, ACCESS_TOKEN_DURATION);
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
        return ACCESS_TOKEN_DURATION;
    }

    @Override
    public Duration getRefreshTokenDuration() {
        return REFRESH_TOKEN_DURATION;
    }

    @Override
    public Duration getJtiDuration() {
        return JTI_DURATION;
    }

    // --- Authentication Info Caching ---
    @Transactional(readOnly = true)
    @Override
    public AuthenticationInfo getAuthenticationInfo(String username) {
        io.micrometer.core.instrument.Timer.Sample sample = Timer.start(meterRegistry);
        String cacheKey = AUTH_CACHE_KEY_PREFIX + username;
        try {
            AuthenticationInfo info = cacheService.getOrCache(
                    AUTH_CACHE,
                    cacheKey,
                    () -> userRepository.findAuthenticationInfoByUsername(username)
                            .orElseThrow(() -> new UsernameNotFoundException("AuthInfo not found for user: " + username)) // Handle not found in supplier
            );
            sample.stop(meterRegistry.timer("auth.info.fetch", "status", "success"));
            return info;
        } catch (Exception e) {
            sample.stop(meterRegistry.timer("auth.info.fetch", "status", "error"));
            log.error("Failed to fetch authentication info for user [{}]: {}", username, e.getMessage(), e);
            throw e; // Re-throw
        }
    }

    @Transactional(noRollbackFor = Exception.class)
    @CacheEvict(value = AUTH_CACHE, key = "'auth:' + #username")
    @Override
    public void updateAuthenticationInfo(String username, AuthenticationInfo newInfo) {
        if (username == null || newInfo == null) {
            throw new IllegalArgumentException("Username and new info cannot be null");
        }
        if (!username.equals(newInfo.getUsername())) {
            throw new IllegalArgumentException("Username mismatch");
        }

        // Fetch existing user to ensure they exist before updating auth info
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Cannot update auth info, user not found: " + username));

        // Update logic might be more complex depending on AuthenticationInfo structure
        // Assuming direct update is possible via repository method
        int updatedRows = userRepository.updateAuthenticationInfo(username, newInfo); // Assuming this method exists and works

        if (updatedRows == 0) {
            // This might happen in race conditions or if the user was deleted between fetch and update
            log.warn("Update authentication info affected 0 rows for user [{}].", username);
            // Consider throwing an exception or handling appropriately
        } else {
            log.info("Updated authentication info for user [{}]", username);
        }
        // REMOVED: cacheService.invalidate(AUTH_CACHE, AUTH_CACHE_KEY_PREFIX + username); - Handled by @CacheEvict
    }

    // --- REMOVED Unused Methods ---
    // private Authentication performAuthentication(LoginRequest loginRequest) { ... }
    // public Authentication createSuccessfulAuthentication(User user) { ... }
}


// TODO: Consider backup codes for account recovery