/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.payload.ErrorResponse; // Use the provided ErrorResponse
import me.amlu.shop.amlume_shop.security.paseto.PasetoClaims; // Import claim constants
import me.amlu.shop.amlume_shop.security.service.DeviceFingerprintService;
// Removed User import as we primarily use userId string
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class DeviceFingerprintVerificationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprintVerificationFilter.class);

    private final DeviceFingerprintService deviceFingerprintService;
    private final ObjectMapper objectMapper;
    private final List<String> excludedUriPatterns;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public DeviceFingerprintVerificationFilter(DeviceFingerprintService deviceFingerprintService,
                                               ObjectMapper objectMapper,
                                               // Inject excluded URIs from properties
                                               @Value("#{'${security.device-fingerprint.excluded-uris:/api/auth/**,/public/**,/error,/actuator/**}'.split(',')}") List<String> excludedUriPatterns) {
        this.deviceFingerprintService = deviceFingerprintService;
        this.objectMapper = objectMapper;
        this.excludedUriPatterns = excludedUriPatterns;
        log.info("DeviceFingerprintVerificationFilter initialized. Excluded URI patterns: {}", excludedUriPatterns);
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {

        // Skip filter if URI matches excluded patterns or if user is not authenticated yet
        if (shouldSkipFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Should already be authenticated by a previous filter (e.g., PasetoAuthenticationFilter)
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("DeviceFingerprintVerificationFilter executed but no authenticated user found for URI: {}", request.getRequestURI());
            // This shouldn't happen if filter order is correct, but handle defensively
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required.");
            return;
        }

        String userId = null; // Initialize to null
        String tokenFingerprint; // Initialize to null

        try {
            // --- Extract userId and fingerprint from Authentication details (assuming a claims map) ---
            // CRITICAL ASSUMPTION: Assumes PasetoAuthenticationFilter (or equivalent)
            // stores the parsed token claims map in authentication.setDetails(claimsMap).
            if (authentication.getDetails() instanceof Map claimsMap) {
                // Use PasetoClaims constants for claim names
                userId = (String) claimsMap.get(PasetoClaims.SUBJECT);
                tokenFingerprint = (String) claimsMap.get(PasetoClaims.DEVICE_FINGERPRINT);
                log.trace("Extracted userId '{}' and fingerprint '{}' from Authentication details map.",
                        userId, tokenFingerprint != null ? "present" : "missing");
            } else {
                // If details are not a map, we cannot extract the necessary info this way.
                log.error("Authentication details are not a Map. Cannot extract device fingerprint. Details type: {}",
                        authentication.getDetails() != null ? authentication.getDetails().getClass().getName() : "null");
                sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "CONFIG_ERROR", "Internal authentication configuration error (details).");
                return;
            }

            // Validate extracted data
            if (userId == null || userId.isBlank()) {
                log.warn("Missing or blank userId (sub claim) in token for authenticated user.");
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Token is missing user information.");
                return;
            }
            if (tokenFingerprint == null || tokenFingerprint.isBlank()) {
                log.warn("Missing or blank device fingerprint ({}) claim in token for user ID {}.", PasetoClaims.DEVICE_FINGERPRINT, userId);
                // Check if fingerprinting is enabled for the user before erroring out completely
                // This requires fetching the user, which might be inefficient here.
                // A simpler approach is to enforce the claim if the filter is active.
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Token is missing required device information.");
                return;
            }

            // --- Perform Verification via Service ---
            log.debug("Verifying device fingerprint for user ID: {}", userId);
            // The service method will generate the current fingerprint and compare
            deviceFingerprintService.verifyDeviceFingerprint(tokenFingerprint, request, userId);
            log.debug("Device fingerprint verification successful for user ID: {}", userId);

            // --- Proceed with the filter chain ---
            filterChain.doFilter(request, response);

        } catch (DeviceFingerprintingDisabledException e) {
            // If the service detects fingerprinting is disabled for the user during verification
            log.trace("Device fingerprinting disabled for user ID {}. Verification skipped by service.", userId);
            filterChain.doFilter(request, response); // Allow request to proceed
        } catch (DeviceFingerprintMismatchException | DeviceFingerprintNotFoundException |
                 InactiveDeviceFingerprintException e) {
            // Handle specific verification failures reported by the service
            log.warn("Device fingerprint verification failed for user ID {}: {}", userId != null ? userId : "UNKNOWN", e.getMessage());
            // Use 401 UNAUTHORIZED as the token's fingerprint doesn't match the current device
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "DEVICE_VERIFICATION_FAILED", e.getMessage());
        } catch (UserNotFoundException e) {
            // Handle the case where user ID from token doesn't exist (should be rare if token validation is robust)
            log.error("User not found during device fingerprint verification for user ID {}", userId != null ? userId : "UNKNOWN", e);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "INVALID_USER", "User associated with token not found.");
        } catch (Exception e) {
            // Catch-all for unexpected errors
            log.error("Unexpected error during device fingerprint verification for user ID {}", userId != null ? userId : "UNKNOWN", e);
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Device verification failed unexpectedly.");
        }
    }

    private boolean shouldSkipFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Check excluded patterns first
        boolean excluded = excludedUriPatterns.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern.trim(), uri));
        if (excluded) {
            log.trace("Skipping DeviceFingerprintVerificationFilter for excluded URI: {}", uri);
            return true;
        }

        // Also skip if no authentication context exists yet
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.trace("Skipping DeviceFingerprintVerificationFilter as no authenticated user found yet for URI: {}", uri);
            return true;
        }

        // Skip, if authentication details are missing (needed for fingerprint extraction),
        // This check is done more thoroughly inside doFilterInternal, but adding a basic one here can be slightly more efficient.
        if (authentication.getDetails() == null) {
            log.trace("Skipping DeviceFingerprintVerificationFilter as authentication details are missing for URI: {}", uri);
            return true; // Cannot proceed without details containing claims
        }

        return false;
    }

    // Use the ErrorResponse structure provided in the context
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String errorCode, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // Assuming ErrorResponse is a record/class with constructor (String errorCode, String message)
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message);
        objectMapper.writeValue(response.getWriter(), errorResponse);
        // No need to flush explicitly, underlying stream usually handles it.
    }
}