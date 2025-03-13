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

import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.model.MfaToken;
import me.amlu.shop.amlume_shop.model.User;
import me.amlu.shop.amlume_shop.payload.user.AuthResponse;
import me.amlu.shop.amlume_shop.payload.user.LoginRequest;
import me.amlu.shop.amlume_shop.repositories.MfaTokenRepository;
import me.amlu.shop.amlume_shop.security.paseto.PasetoTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static me.amlu.shop.amlume_shop.commons.Constants.*;

//The AuthService should be solely responsible for authentication (verifying credentials, generating tokens, managing sessions)

@Slf4j
@Service
public class AuthServiceImpl extends BaseService implements AuthService {

    //    private static final String AUTH_HEADER = "Authorization";
//    private static final String BEARER_PREFIX = "Bearer ";
    private static final Duration ACCESS_TOKEN_DURATION = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(7);

    private final AuthenticationManager authenticationManager;
    private final PasetoTokenService pasetoTokenService;
    private final MfaService mfaService;
    private final MfaTokenRepository mfaTokenRepository;

    private final HttpServletRequest request;
    private final String fingerprintSalt;

    public AuthServiceImpl(AuthenticationManager authenticationManager, PasetoTokenService pasetoTokenService, MfaService mfaService, MfaTokenRepository mfaTokenRepository,
                           HttpServletRequest request, @Value("${FINGERPRINT_SALT}") String fingerprintSalt) {
        this.authenticationManager = authenticationManager;
        this.pasetoTokenService = pasetoTokenService;
        this.mfaService = mfaService;
        this.mfaTokenRepository = mfaTokenRepository;
        this.request = request;
        this.fingerprintSalt = fingerprintSalt;
    }

    @Override
    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        validateLoginRequest(loginRequest);

        try {
            Authentication authentication = performAuthentication(loginRequest);
            User user = extractAndValidateUser(authentication);
            return handleUserAuthentication(user);
        } catch (AuthenticationException e) {
            logAuthenticationFailure(loginRequest.getUsername(), e);
            throw new AuthenticationException("Invalid credentials");
        }
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

    private User extractAndValidateUser(Authentication authentication) {
        return Optional.ofNullable(authentication.getPrincipal())
//                .map(principal -> (User) principal)
                .map(User.class::cast)
                .orElseThrow(() -> new AuthenticationException("Authentication failed"));
    }

    private AuthResponse handleUserAuthentication(User user) {
        return isMfaRequired(user)
                ? initiateMfaChallenge(user)
                : generateAuthenticationResponse(user);
    }

    private boolean isMfaRequired(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        return mfaService.isMfaEnabled(user) || mfaService.isMfaEnforced(user);
    }

    private void logAuthenticationFailure(String username, AuthenticationException e) {
        log.error("Authentication failed for user: {} - Reason: {}",
                username,
                e.getMessage(),
                e);
    }

    @Override
    public AuthResponse initiateMfaChallenge(User user) {
        try {
            validateUser(user);
            MfaToken mfaToken = retrieveOrCreateMfaToken(user);
            return buildMfaResponse(mfaToken);
        } catch (Exception e) {
            handleMfaError(user, e);
            throw new MfaSetupException("Failed to initiate MFA challenge", e);
        }
    }

    private void validateUser(User user) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("Invalid user data");
        }
    }

    private MfaToken retrieveOrCreateMfaToken(User user) {
        return mfaTokenRepository.findByUser(user)
                .orElseGet(() -> createNewMfaToken(user));
    }

    private MfaToken createNewMfaToken(User user) {
        String secret = mfaService.generateSecretKey();
        return mfaTokenRepository.save(
                MfaToken.builder()
                        .user(user)
                        .secret(secret)
                        .enabled(false)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build()
        );
    }

    private AuthResponse buildMfaResponse(MfaToken mfaToken) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("mfaRequired", true);

        if (!mfaToken.isEnabled()) {
            addMfaSetupDetails(details, mfaToken);
        }

        return AuthResponse.builder()
                .token(null)
                .details(details)
                .mfaRequired(true)
                .build();
    }


    private void addMfaSetupDetails(Map<String, Object> details, MfaToken mfaToken) {
        String qrCodeUrl = mfaService.generateQrCodeImageUrl(mfaToken.getUser(), mfaToken.getSecret());
        details.put("secret", mfaToken.getSecret());
        details.put("qrCodeUrl", qrCodeUrl);
    }

    private void handleMfaError(User user, Exception e) {
        log.error("MFA challenge failed for user: {} - Error: {}",
                user.getUserId(),
                e.getMessage(),
                e);
    }

    @Override
    public AuthResponse validateMfa(String code)
            throws MfaVerificationException, TooManyAttemptsException {
        User user = userService.getCurrentUser();

        try {
            if (mfaService.isUserLocked(user)) {
                throw new LockedException("Account is locked."); // Provide more information
            }

            MfaToken mfaToken = mfaTokenRepository.findByUser(user)
                    .orElseThrow(() -> new MfaNotSetupException("MFA not enabled for this user."));

            if (!mfaService.verifyCode(mfaToken.getSecret(), code)) { // Correct secret usage
                mfaService.recordFailedAttempt(user);
                throw new MfaValidationException("Invalid MFA code.");
            }

            mfaService.resetFailedAttempts(user); // Clear failed attempts on successful validation

            // Return token for regular login if successful validation
            return generateAuthenticationResponse(user);
        } catch (Exception e) {
            assert user != null;
            log.error("MFA verification failed for user: {}", user.getUserId(), e
            );
            throw new MfaVerificationException("Failed to verify MFA code");
        }
    }

    private AuthResponse generateAuthenticationResponse(User user) {
//        if (user == null) {
//            throw new IllegalArgumentException("User cannot be null");
//        }

        return AuthResponse.builder()
                .token(pasetoTokenService.generatePublicAccessToken(
                        String.valueOf(user.getUserId()),
                        ACCESS_TOKEN_DURATION
                ))
                .username(user.getUsername())
                .authorities(user.getAuthorities())
                .success(true)
                .message("Login successful")
                .build();
    }

    @Override
    public String generateDeviceFingerprint() {
        try {
            Map<String, String> data = new HashMap<>(); // Use a map for ordered data

            // Add data points (handle nulls and empty values)
            addData(data, USER_AGENT, request.getHeader(USER_AGENT));
            addData(data, ACCEPT_LANGUAGE, request.getHeader(ACCEPT_LANGUAGE));
            addData(data, "Platform", getPlatform(request.getHeader(USER_AGENT)));
            addData(data, "Time-Zone", getTimeZone(request));
            addData(data, "IP-address", request.getRemoteAddr());
            addData(data, "Session-ID", request.getSession().getId());

            // Check if data is empty
            if (data.isEmpty()) {
                return UUID.randomUUID().toString(); // Fallback if no data
            }

            // Normalize and order data
            List<String> sortedKeys = new ArrayList<>(data.keySet());
            Collections.sort(sortedKeys);

            StringBuilder rawFingerprint = new StringBuilder();
            for (String key : sortedKeys) {
                rawFingerprint.append(key).append(": ").append(data.get(key)).append("\n");
            }

            // Add salt and hash
            String saltedFingerprint = rawFingerprint.toString() + fingerprintSalt; // Add the salt
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(saltedFingerprint.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString(); // Fallback if SHA-256 is not available
        }
    }

    private void addData(Map<String, String> data, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            data.put(key, value);
        }
    }

    private String getPlatform(String userAgent) {
        if (StringUtils.isBlank(userAgent)) {
            return null;
        }

        // Convert to lowercase once and store
        String userAgentLower = userAgent.toLowerCase(Locale.ENGLISH);

        // Use switch for better performance with string matching
        if (userAgentLower.contains("windows")) {
            return "Windows";
        }

        if (userAgentLower.contains("mac")) {  // Simplified mac detection
            return "macOS";
        }

        if (userAgentLower.contains("linux")) {
            return "Linux";
        }

        if (userAgentLower.contains("android")) {
            return "Android";
        }

        if (userAgentLower.contains("ios")) {
            return "iOS";
        }

        return "Other"; // Or handle unknown platforms differently
    }


    private String getTimeZone(HttpServletRequest request) {
        // Get the timezone from the request header
        return Optional.ofNullable(request.getHeader(TIME_ZONE))
                .orElse(ZoneOffset.UTC.getId());  // Provide default timezone
    }

    @Override
    public String determineUserScope() {
        // Get the current authenticated user's authorities
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "ROLE_ANONYMOUS";
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

}