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
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.commons.Constants;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.model.MfaToken;
import me.amlu.shop.amlume_shop.service.CacheService;
import me.amlu.shop.amlume_shop.user_management.AuthenticationInfo;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.model.UserDeviceFingerprint;
import me.amlu.shop.amlume_shop.payload.user.*;
import me.amlu.shop.amlume_shop.repositories.MfaTokenRepository;
import me.amlu.shop.amlume_shop.repositories.UserDeviceFingerprintRepository;
import me.amlu.shop.amlume_shop.resilience.service.ResilienceService;
import me.amlu.shop.amlume_shop.security.paseto.PasetoTokenService;
import me.amlu.shop.amlume_shop.security.paseto.PasetoTokenServiceImpl;
import me.amlu.shop.amlume_shop.user_management.UserRepository;
import me.amlu.shop.amlume_shop.user_management.UserService;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static me.amlu.shop.amlume_shop.commons.Constants.AUTH_CACHE;
import static me.amlu.shop.amlume_shop.commons.Constants.AUTH_CACHE_KEY_PREFIX;

@Service
@Slf4j
@Transactional
public class EnhancedAuthenticationServiceImpl implements EnhancedAuthenticationService {

    private static final int ACCESS_TOKEN_DURATION_INT = 15;
    private static final String ACCOUNT_LOCKED_MESSAGE = "Account is locked";
    private static final String USER_NOT_FOUND_MESSAGE = "User not found";
    private static final Duration ACCESS_TOKEN_DURATION = Duration.ofMinutes(ACCESS_TOKEN_DURATION_INT);
    private static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(7);
    private static final Duration JTI_DURATION = Duration.ofMinutes(ACCESS_TOKEN_DURATION_INT + (long) 1);
    private final String fingerprintSalt;

    private final AuthenticationManager authenticationManager;
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
    private final LoginAttemptService loginAttemptService;
    private final CaptchaService captchaService;
    private final SecurityAuditService auditService;
    private final SecurityNotificationService notificationService;

    public EnhancedAuthenticationServiceImpl(@Value("${FINGERPRINT_SALT}") String fingerprintSalt, AuthenticationManager authenticationManager, CacheService cacheService, MeterRegistry meterRegistry, UserService userService, UserDeviceFingerprintRepository userDeviceFingerprintRepository, DeviceFingerprintService deviceFingerprintService, HttpServletRequest httpServletRequest, ResilienceService resilienceService, LoginAttemptService loginAttemptService, CaptchaService captchaService, SecurityAuditService auditService, SecurityNotificationService notificationService, UserRepository userRepository, PasswordEncoder passwordEncoder, MfaService mfaService, MfaTokenRepository mfaTokenRepository, PasetoTokenServiceImpl pasetoTokenService) {
        this.fingerprintSalt = fingerprintSalt;
        this.authenticationManager = authenticationManager;
        this.cacheService = cacheService;
        this.meterRegistry = meterRegistry;
        this.userService = userService;
        this.userDeviceFingerprintRepository = userDeviceFingerprintRepository;
        this.deviceFingerprintService = deviceFingerprintService;
        this.httpServletRequest = httpServletRequest;
        this.resilienceService = resilienceService;
        this.loginAttemptService = loginAttemptService;
        this.captchaService = captchaService;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mfaService = mfaService;
        this.mfaTokenRepository = mfaTokenRepository;
        this.pasetoTokenService = pasetoTokenService;
    }

    public AuthResponse register(@Valid UserRegistrationRequest request, String ipAddress) throws TooManyAttemptsException, InvalidCaptchaException {
        try {
            // 0. Pre-flight checks, resilience
            performRegistrationPreFlightChecks(request, ipAddress);

            User user = userService.registerUser(request);

            // if MFA enabled --> Generate Secret
            if (request.isMfaEnabled()) {
                mfaService.enableMfaForUser(user);
                MfaToken mfaToken = mfaTokenRepository.findByUser(user).orElseThrow(() -> new MfaTokenNotFoundException("MFA Token not found"));
            }

//            userRepository.save(user); // User already saved in userService.registerUser

            // Generate tokens
            AuthTokenGenerator generateAuthTokens = getGeneratedAuthToken(user);

            return AuthResponse.builder()
                    .secretImageUrl(mfaService.generateQrCodeImageUrl(user, mfaToken.getSecret()))
                    .accessToken(generateAuthTokens.accessToken())
                    .refreshToken(generateAuthTokens.refreshToken())
                    .mfaEnabled(user.isMfaEnabled())
                    .build();
        } catch (TooManyAttemptsException | InvalidCaptchaException e) {
            throw new TooManyAttemptsException(e.getMessage());
        } catch (UserAlreadyExistsException e) {
            throw e; // Re-throw if it's a specific exception you want to handle
        } catch (Exception e) {
            log.error("Error during registration", e);
            throw new UserRegistrationException("Registration failed"); // Or a more generic exception
        }
    }

    @NotNull
    private AuthTokenGenerator getGeneratedAuthToken(User user) {
        String userId = String.valueOf(user.getUserId());
        var accessToken = pasetoTokenService.generatePublicAccessToken(userId, ACCESS_TOKEN_DURATION);
        var refreshToken = pasetoTokenService.generateRefreshToken(user);
        AuthTokenGenerator generateAuthTokens = new AuthTokenGenerator(accessToken, refreshToken);
        return generateAuthTokens;
    }

    private record AuthTokenGenerator(String accessToken, String refreshToken) {
    }

    @Override
    public AuthResponse authenticateUser(@Valid LoginRequest request, String ipAddress) throws TooManyAttemptsException, ExecutionException, InvalidCaptchaException {
        try {
            // 0. Pre-flight checks, resilience
            validateLoginRequest(request);
            performLoginPreFlightChecks(request, ipAddress);

            // 1. Authenticate user
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE));
            if (user == null) {
                auditService.logFailedLogin(
                        request.getUsername(),
                        ipAddress,
                        USER_NOT_FOUND_MESSAGE
                );
                throw new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE);
            }

            // Check if account is locked
            if (!user.isAccountNonLocked()) {
                if (!unlockWhenTimeExpired(user)) {
                    auditService.logFailedLogin(
                            request.getUsername(),
                            ipAddress,
                            ACCOUNT_LOCKED_MESSAGE
                    );
                    throw new LockedException(ACCOUNT_LOCKED_MESSAGE);
                }
                handleLockedAccount(user, ipAddress);
                return new AuthResponse(ACCOUNT_LOCKED_MESSAGE, null, false);
            }

            // Check if password is correct
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                handleFailedLogin(user, ipAddress);
                throw new BadCredentialsException("Invalid credentials");
            }

            // Password is correct, now handle MFA if required
            if (mfaService.isMfaEnabled(user) || mfaService.isMfaEnforced(user)) {
                // Convert LoginRequest to AuthenticationRequest
                AuthenticationRequest authRequest = new AuthenticationRequest(
                        request.getUsername(),
                        request.getPassword(),
                        request.getCaptchaResponse()
                );
                return handleMfaAuthentication(user, authRequest, ipAddress);

            } else {
                // Generate device fingerprint
                String deviceFingerprint = generateAndHandleFingerprint(request.getUserAgent(), request.getScreenWidth(), request.getScreenHeight());

                return handleSuccessfulLogin(user, ipAddress, deviceFingerprint);
            }

        } catch (TooManyAttemptsException | InvalidCaptchaException e) {
            return new AuthResponse(e.getMessage(), null, false);
        } catch (Exception e) {
            log.error("Authentication error", e);
            return new AuthResponse("Authentication failed", null, false);
        }
    }

    public AuthResponse logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(httpServletRequest, null, authentication);
        }

        // TODO: add more logic as token invalidation/revocation, etc.
        return new AuthResponse(null, null, null, null, null, false, false, null, true, null, null);

    }

    private void validateLoginRequest(LoginRequest loginRequest) {
        if (loginRequest == null ||
                StringUtils.isBlank(loginRequest.getUsername()) ||
                StringUtils.isBlank(loginRequest.getPassword())) {
            throw new IllegalArgumentException("Invalid login request");
        }
    }

    private Authentication performAuthentication(LoginRequest loginRequest) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername().trim(),
                        loginRequest.getPassword()
                )
        );
    }

    private void performRegistrationPreFlightChecks(UserRegistrationRequest request, String ipAddress) throws TooManyAttemptsException, InvalidCaptchaException {
        //        // Old IP rate limiting check using Guava. Use as fallback
//        try {
//            ipRateLimitingService.checkIpRate(ipAddress);
//        } catch (TooManyAttemptsException e) {
//            auditService.logFailedLogin(request.getUsername(), ipAddress, "Too Many Attempts per IP");
//            throw e; // Re-throw the exception
//        }

        // Check IP rate limiting and login attempts
        if (resilienceService.allowRequestByIp(ipAddress) && resilienceService.allowRequestByUsername(request.getUsername())) {

            // Check if we need to wait (exponential backoff)
            try {
                loginAttemptService.waitIfRequired(request.getUsername());
            } catch (TooManyAttemptsException e) {
                auditService.logFailedLogin(request.getUsername(), ipAddress, "Too Many Login Attempts");
                throw e; // Re-throw the exception
            }

            // Validate CAPTCHA if required
            if (request.getCaptchaResponse() != null && !captchaService.validateCaptcha(request.getCaptchaResponse(), ipAddress)) {
                auditService.logFailedLogin(request.getUsername(), ipAddress, "Invalid CAPTCHA");
                throw new InvalidCaptchaException("Invalid CAPTCHA");
            }

        } else {
            // Rate limited (either IP or user), return appropriate error response
            auditService.logFailedLogin(request.getUsername(), ipAddress, "Too Many Attempts per IP");
            throw new TooManyAttemptsException("Too many requests from this IP. Please try again later.");
        }
    }


    private void performLoginPreFlightChecks(LoginRequest request, String ipAddress) throws TooManyAttemptsException, InvalidCaptchaException {
//        // Old IP rate limiting check using Guava. Use as fallback
//        try {
//            ipRateLimitingService.checkIpRate(ipAddress);
//        } catch (TooManyAttemptsException e) {
//            auditService.logFailedLogin(request.getUsername(), ipAddress, "Too Many Attempts per IP");
//            throw e; // Re-throw the exception
//        }

        // Check IP rate limiting and login attempts
        if (resilienceService.allowRequestByIp(ipAddress) && resilienceService.allowRequestByUsername(request.getUsername())) {

            // Check if we need to wait (exponential backoff)
            try {
                loginAttemptService.waitIfRequired(request.getUsername());
            } catch (TooManyAttemptsException e) {
                auditService.logFailedLogin(request.getUsername(), ipAddress, "Too Many Login Attempts");
                throw e; // Re-throw the exception
            }

            // Validate CAPTCHA if required
            if (request.getCaptchaResponse() != null && !captchaService.validateCaptcha(request.getCaptchaResponse(), ipAddress)) {
                auditService.logFailedLogin(request.getUsername(), ipAddress, "Invalid CAPTCHA");
                throw new InvalidCaptchaException("Invalid CAPTCHA");
            }

        } else {
            // Rate limited (either IP or user), return appropriate error response
            auditService.logFailedLogin(request.getUsername(), ipAddress, "Too Many Attempts per IP");
            throw new TooManyAttemptsException("Too many requests from this IP. Please try again later.");
        }
    }

    @Override
    public AuthResponse handleMfaAuthentication(User user, AuthenticationRequest request, String ipAddress) throws TooManyAttemptsException, ExecutionException {
        MfaToken mfaToken = mfaTokenRepository.findByUser(user)
                .orElseGet(() -> initializeMfaToken(user));

        // If MFA is not yet set up or verification is needed
        if (!mfaToken.isEnabled() || request.getMfaCode() == null) {
            return initiateMfaChallenge(user);
        }

        // Verify MFA code
        if (!mfaService.verifyCode(mfaToken.getSecret(), request.getMfaCode())) {
            auditService.logMfaVerificationFailed(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);
            return new AuthResponse(null, "Invalid MFA code", false);
        }

        String deviceFingerprint = generateAndHandleFingerprint(request.getUserAgent(), request.getScreenWidth(), request.getScreenHeight());

        return handleSuccessfulLogin(user, ipAddress, deviceFingerprint);
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
                    .orElseGet(() -> {
                        String secret = mfaService.generateSecretKey();
                        MfaToken newToken = new MfaToken(user, secret, mfaService.isMfaEnforced(user));
                        return mfaTokenRepository.save(newToken);
                    });

            Map<String, Object> details = new HashMap<>();
            details.put("mfaRequired", true);

            if (!mfaToken.isEnabled()) {
                String qrCodeUrl = mfaService.generateQrCodeImageUrl(user, mfaToken.getSecret());
                details.put("secret", mfaToken.getSecret());
                details.put("qrCodeUrl", qrCodeUrl);
                details.put("setupRequired", true);
            }

            return new AuthResponse(null, details.toString(), true);
        } catch (Exception e) {
            log.error("Error initiating MFA challenge for user: {}", user.getUsername(), e);
            throw new MfaException(MfaException.MfaErrorType.CHALLENGE_FAILED, "Failed to initiate MFA challenge");
        }
    }

    @Override
    public AuthResponse verifyMfaAndLogin(MfaVerificationRequest request, String ipAddress) throws TooManyAttemptsException, ExecutionException {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE));

        MfaToken mfaToken = mfaTokenRepository.findByUser(user)
                .orElseThrow(() -> new MfaException(MfaException.MfaErrorType.SETUP_FAILED, "MFA not set up for user"));

        if (mfaService.verifyCode(mfaToken.getSecret(), request.getMfaCode())) {
            String deviceFingerprint = generateAndHandleFingerprint(request.getUserAgent(), request.getScreenWidth(), request.getScreenHeight());

            return handleSuccessfulLogin(user, ipAddress, deviceFingerprint);
        } else {
            auditService.logMfaChallengeFailed(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);
            return new AuthResponse(null, "Invalid MFA code", false);
        }
    }

    @Override
    public AuthResponse handleSuccessfulLogin(User user, String ipAddress, String deviceFingerprint) {
        resetFailedAttempts(user);
        user.setLastLoginTime(Instant.now());
        userRepository.save(user);
        auditService.logSuccessfulLogin(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);

        // Generate tokens
        String userId = String.valueOf(user.getUserId());
        String accessToken = pasetoTokenService.generatePublicAccessToken(userId, ACCESS_TOKEN_DURATION);
        String refreshToken = pasetoTokenService.generateRefreshToken(user);

        // Check EXISTING device fingerprints for the user
        if (deviceFingerprint != null) {
            deviceFingerprintService.storeOrUpdateFingerprint(user, accessToken, refreshToken, deviceFingerprint);
        }

        if (deviceFingerprint != null) {
            associateFingerprintWithTokens(user, accessToken, refreshToken, deviceFingerprint); // New method
        }

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .authorities(user.getAuthorities())
                .success(true)
                .message("Login successful")
                .build();
    }

    @Override
    public void handleFailedLogin(User user, String ipAddress) {
        increaseFailedAttempts(user);
        auditService.logFailedLogin(
                user.getUsername(),
                ipAddress,
                "Invalid credentials"
        );

        if (user.getFailedLoginAttempts() >= Constants.MAX_FAILED_ATTEMPTS) {
            lockUser(user);
            notificationService.sendAccountLockedEmail(user);
            auditService.logAccountLocked(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);
        }
    }

    @Override
    public void handleLockedAccount(User user, String ipAddress) {
        if (unlockWhenTimeExpired(user)) {
            auditService.logAccountUnlocked(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);
        } else {
            auditService.logAccessDeniedLockedAccount(String.valueOf(user.getUserId()), user.getUsername(), ipAddress);
            throw new LockedException(ACCOUNT_LOCKED_MESSAGE);
        }
    }

    private void associateFingerprintWithTokens(User user, String accessToken, String refreshToken, String deviceFingerprint) {
        // 1. Store the association in your database
        UserDeviceFingerprint userDeviceFingerprint = new UserDeviceFingerprint();
        userDeviceFingerprint.setUser(user);
        userDeviceFingerprint.setDeviceFingerprint(deviceFingerprint);
        userDeviceFingerprint.setAccessToken(accessToken); // Store the access token
        userDeviceFingerprint.setRefreshToken(refreshToken); // Store the refresh token
        userDeviceFingerprint.setLastUsedAt(Instant.now());
        userDeviceFingerprintRepository.save(userDeviceFingerprint);

        // 2. Or, if there are a different way to associate, do it here.
    }

    @Override
    public void resetFailedAttempts(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);
    }

    @Override
    public void increaseFailedAttempts(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        userRepository.save(user);
    }

    @Override
    public void lockUser(User user) {
        user.setAccountNonLocked(false);
        user.setLockTime(Instant.now());
        userRepository.save(user);
    }

    @Override
    public void unlockUser(String username) { // Implement the logic to unlock the user (e.g. after a time period or by admin intervention)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE));
        user.setAccountNonLocked(true);
        user.setFailedLoginAttempts(0); // Reset failed attempts
        userRepository.save(user);
    }

    @Override
    public boolean unlockWhenTimeExpired(User user) {
        if (user.getLockTime() != null) {
            long lockTimeInMillis = user.getLockTime().toEpochMilli();
            long currentTimeInMillis = System.currentTimeMillis();

            if (currentTimeInMillis - lockTimeInMillis >= Constants.LOCK_DURATION_MILLIS) {
                user.setAccountNonLocked(true);
                user.setLockTime(null);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    @Override
    public Authentication createSuccessfulAuthentication(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName().name()))
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    @Override
    public String generateAndHandleFingerprint(String userAgent, String screenWidth, String screenHeight) {
        String deviceFingerprint = deviceFingerprintService.generateDeviceFingerprint(userAgent, screenWidth, screenHeight, httpServletRequest);

        if (deviceFingerprint == null) {
            log.warn("Device fingerprint is missing.");
            // Handle as needed (allow login or not)
            return null; // or perhaps a default value if you allow login without it
        }
        return deviceFingerprint;
    }

    // TOCHECK: ROLE_ANONYMOUS
    @Override
    public String determineUserScope() {
        // Get the current authenticated user's authorities
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new UnauthorizedException("No authenticated user found");
//            return "ROLE_ANONYMOUS";
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));
    }

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

    @Transactional(readOnly = true)
    public AuthenticationInfo getAuthenticationInfo(String username) {
        io.micrometer.core.instrument.Timer.Sample sample = Timer.start(meterRegistry);
        try {
            AuthenticationInfo info = cacheService.getOrCache(
                    AUTH_CACHE,
                    AUTH_CACHE_KEY_PREFIX + username,
                    () -> userRepository.findAuthenticationInfoByUsername(username)
            );
            sample.stop(meterRegistry.timer("auth.info.fetch",
                    "status", "success"));
            return info;
        } catch (Exception e) {
            sample.stop(meterRegistry.timer("auth.info.fetch",
                    "status", "error"));
            throw e;
        }
    }

    @Transactional(noRollbackFor = Exception.class)
    @CacheEvict(value = AUTH_CACHE, key = "'auth:' + #username")
    public void updateAuthenticationInfo(String username, AuthenticationInfo newInfo) {
        if (username == null || newInfo == null) {
            throw new IllegalArgumentException("Username and new info cannot be null");
        }

        if (!username.equals(newInfo.getUsername())) {
            throw new IllegalArgumentException("Username mismatch");
        }

        userRepository.updateAuthenticationInfo(username, newInfo);
        cacheService.invalidate(AUTH_CACHE, AUTH_CACHE_KEY_PREFIX + username);
    }

//    @Override
//    public void logout(HttpServletRequest request, HttpServletResponse response) {
//        // Invalidate the current session
//        request.getSession().invalidate();
//
//        // Clear the security context
//        SecurityContextHolder.clearContext();
//
//        // Clear the authentication cookie
//        CookieUtils.clearCookie(request, response, "auth_token");
//
//        // Redirect to the login page or any other desired page
//        try {
//            response.sendRedirect("/login");
//        } catch (IOException e) {
//            // Handle the exception appropriately
//            e.printStackTrace();
//        }
}


// TODO: Consider backup codes for account recovery