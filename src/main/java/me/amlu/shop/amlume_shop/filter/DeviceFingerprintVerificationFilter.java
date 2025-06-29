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
import me.amlu.shop.amlume_shop.dto.ErrorResponse;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.security.paseto.PasetoClaims;
import me.amlu.shop.amlume_shop.security.service.DeviceFingerprintService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * {@code DeviceFingerprintVerificationFilter} is a Spring {@link OncePerRequestFilter} that
 * intercepts incoming HTTP requests to verify the device fingerprint associated with
 * authenticated users. This filter ensures that the device from which an authenticated
 * request originates matches the device fingerprint stored in the user's authentication token.
 *
 * <p>It integrates with Spring Security and relies on the authentication details
 * (specifically, a map of claims from a token like PASETO) to extract the user ID and
 * the device fingerprint claim.
 *
 * <p>The filter can be configured with a list of URI patterns to exclude from
 * device fingerprint verification, typically for public endpoints, authentication
 * endpoints, or actuator endpoints.
 *
 * <p>If a mismatch or missing fingerprint is detected for an authenticated user,
 * an appropriate error response (HTTP 401 Unauthorized) is sent.
 *
 * @see DeviceFingerprintService
 * @see PasetoClaims
 */
@Component
public class DeviceFingerprintVerificationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprintVerificationFilter.class);

    private final DeviceFingerprintService deviceFingerprintService;
    private final ObjectMapper objectMapper;
    private final List<String> excludedUriPatterns;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public DeviceFingerprintVerificationFilter(DeviceFingerprintService deviceFingerprintService,
                                               ObjectMapper objectMapper,
                                               @Value("#{'${security.device-fingerprint.excluded-uris:/api/auth/**,/public/**,/error,/actuator/**}'.split(',')}") List<String> excludedUriPatterns) {
        this.deviceFingerprintService = deviceFingerprintService;
        this.objectMapper = objectMapper;

        // Defensive copy of the list to ensure immutability and prevent external modification
        // Although @Value injects a new list, it's good practice for collections.
        // Using List.copyOf (Java 10+) or Collections.unmodifiableList for older versions.
        // For this case, since it's injected and used internally, a direct assignment is fine,
        // but if this list were exposed or modified, a defensive copy would be crucial.
        // For now, assuming the injected list is not modified elsewhere.
        // If the list could be modified after injection, consider:
        // this.excludedUriPatterns = List.copyOf(excludedUriPatterns); // Java 10+
        // or this.excludedUriPatterns = Collections.unmodifiableList(new ArrayList<>(excludedUriPatterns));
        this.excludedUriPatterns = excludedUriPatterns;
        log.info("DeviceFingerprintVerificationFilter initialized. Excluded URI patterns: {}", excludedUriPatterns);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Validate input parameters to ensure they are not null, though @NonNull helps,
        // it's a compile-time annotation. Runtime checks can add robustness.
        // However, Spring's filter chain typically ensures these are not null.
        // if (request == null || response == null || filterChain == null) {
        // log.error("Null argument passed to doFilterInternal. Request: {}, Response: {}, FilterChain: {}",
        // request, response, filterChain);
        // // Depending on policy, might throw an exception or send an error.
        // // For a filter, throwing ServletException is appropriate.
        // throw new ServletException("Invalid filter invocation: null arguments.");
        // }

        // Log the start of the filter for debugging purposes
        log.debug("Entering DeviceFingerprintVerificationFilter for request URI: {}", request.getRequestURI());

        // Skip filter if URI matches excluded patterns or if user is not authenticated yet
        if (shouldSkipFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // This check is duplicated with shouldSkipFilter, but kept for clarity within doFilterInternal's main logic.

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            // User is anonymous or not authenticated, skip fingerprint check
            filterChain.doFilter(request, response);
            return;
        }

        // Proceed with fingerprint logic only for authenticated users
        Object details = authentication.getDetails();

        // Early exit if details are null, as we cannot proceed without them.
        if (details == null) {
            log.warn("Authentication details are null for authenticated user. Skipping device fingerprint verification for user: {}", authentication.getName());
            filterChain.doFilter(request, response); // Allow to proceed, as this might be an expected scenario for some auth types.
            return;
        }
        String userId = null; // Initialize to null
        String tokenFingerprint = ""; // Initialize to null

        try {
            // --- Extract userId and fingerprint from Authentication details (assuming a claims map) ---
            // CRITICAL ASSUMPTION: Assumes PasetoAuthenticationFilter (or equivalent)
            // stores the parsed token claims map in authentication.setDetails(claimsMap).

            // Verifying type
            if (details instanceof Map) {
                Map<String, Object> detailsMap = (Map<String, Object>) details;
                // Using PasetoClaims constants for claim names for consistency and type safety.
                // Ensure PasetoClaims.SUBJECT and PasetoClaims.DEVICE_FINGERPRINT are correctly defined.
                // Example: public static final String SUBJECT = "sub";
                // public static final String DEVICE_FINGERPRINT = "device_fingerprint";
                // Use PasetoClaims constants for claim names
                userId = (String) detailsMap.get("sub");
//                userId = (String) detailsMap.get(PasetoClaims.SUBJECT); // For PASETO usage
                tokenFingerprint = (String) detailsMap.get("device_fingerprint");
//                tokenFingerprint = (String) detailsMap.get(PasetoClaims.DEVICE_FINGERPRINT); // For PASETO usage
                log.trace("Extracted userId '{}' and fingerprint '{}' from Authentication details map.",
                        userId, tokenFingerprint != null ? "present" : "missing");
            } else {
                // If details are not a map, we cannot extract the necessary info this way. Skip the filter.
                log.warn("Authentication details are not a Map (expected for claims). Skipping device fingerprint verification. Details type: {}",
                        details.getClass().getName());

                // This scenario indicates a misconfiguration or an unexpected authentication flow.
                // Depending on your security policy, you might:
                // 1. Allow the request (if fingerprint is optional)
                // 2. Deny the request (if fingerprint is mandatory for authenticated users)
                // If denying:
                // response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication details for fingerprint verification.");
                // return;
//                sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "CONFIG_ERROR", "Internal authentication configuration error (details).");
                filterChain.doFilter(request, response); // Allow to proceed, as this might be an expected scenario for some auth types.
            }

            // Validate extracted data
            if (userId == null || userId.isBlank()) {
                log.warn("Missing or blank userId (sub claim) in token for authenticated user.");
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Token is missing user information.");
                return;
            }
            if (tokenFingerprint == null || tokenFingerprint.isBlank()) { // Use PasetoClaims.DEVICE_FINGERPRINT for logging consistency
//                log.warn("Missing or blank device fingerprint ({}) claim in token for user ID {}.", PasetoClaims.DEVICE_FINGERPRINT, userId);
                // Check if fingerprinting is enabled for the user before erroring out completely
                // This requires fetching the user, which might be inefficient here.
                // A simpler approach is to enforce the claim if the filter is active.
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Token is missing required device information.");
                return;
            }

            // --- Perform Verification via Service ---
            log.debug("Verifying device fingerprint for user ID: {}", userId);
            // The service method will generate the current fingerprint from the request and compare it with the token's fingerprint.
            deviceFingerprintService.verifyDeviceFingerprint(tokenFingerprint, request, userId);
            log.debug("Device fingerprint verification successful for user ID: {}", userId);

            // --- Proceed with the filter chain ---
            filterChain.doFilter(request, response);

        } catch (DeviceFingerprintingDisabledException e) {
            // If the service detects fingerprinting is disabled for the user during verification
            // This is a valid scenario where fingerprinting is not enforced for a specific user.
            log.info("Device fingerprinting disabled for user ID {}. Verification skipped by service.", userId);
            filterChain.doFilter(request, response); // Allow request to proceed
        } catch (DeviceFingerprintMismatchException | DeviceFingerprintNotFoundException |
                 InactiveDeviceFingerprintException e) {
            // Handle specific verification failures reported by the service
            log.warn("Device fingerprint verification failed for user ID {}: {}", userId != null ? userId : "UNKNOWN", e.getMessage());
            // Use 401 UNAUTHORIZED as the token's fingerprint doesn't match the current device
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "DEVICE_VERIFICATION_FAILED", e.getMessage());
        } catch (UserNotFoundException e) {
            // Handle the case where user ID from token doesn't exist (should be rare if token validation is robust)
            // This could indicate a revoked user or a stale token.
            log.error("User not found during device fingerprint verification for user ID {}", userId != null ? userId : "UNKNOWN", e);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "INVALID_USER", "User associated with token not found.");
        } catch (Exception e) {
            // Catch-all for unexpected errors
            log.error("Unexpected error during device fingerprint verification for user ID {}", userId != null ? userId : "UNKNOWN", e);
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Device verification failed unexpectedly.");
        } finally { // Ensure logging of filter completion
            log.debug("Exiting DeviceFingerprintVerificationFilter for request URI: {}", request.getRequestURI());
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

        // Also skip if no authentication context exists yet or if it's an anonymous user.
        // This check is crucial to allow unauthenticated requests to pass through
        // and for Spring Security's authentication filters to do their job first.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            log.trace("Skipping DeviceFingerprintVerificationFilter as no authenticated or non-anonymous user found yet for URI: {}", uri);
            return true;
        }

        return false;
    }

    /**
     * Sends an error response to the client using the predefined {@link ErrorResponse} DTO.
     * The response content type is set to JSON.
     *
     * @param response  The {@link HttpServletResponse} to write the error to.
     * @param status    The HTTP status code to set for the response.
     * @param errorCode A specific error code string for the client to interpret.
     * @param message   A human-readable message describing the error.
     * @throws IOException If an I/O error occurs while writing the response.
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String errorCode, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // Assuming ErrorResponse is a record/class with constructor (String errorCode, String message)
        // Ensure ErrorResponse is correctly defined and accessible.
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message);
        objectMapper.writeValue(response.getWriter(), errorResponse);
        // No need to flush explicitly, underlying stream usually handles it.
    }
}
