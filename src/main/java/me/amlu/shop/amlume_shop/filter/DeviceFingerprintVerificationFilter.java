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
import me.amlu.shop.amlume_shop.model.UserDeviceFingerprint;
import me.amlu.shop.amlume_shop.repositories.UserDeviceFingerprintRepository;
import me.amlu.shop.amlume_shop.security.service.SecurityAuditService;
import me.amlu.shop.amlume_shop.user_management.User;
import me.amlu.shop.amlume_shop.security.service.DeviceFingerprintService;
import me.amlu.shop.amlume_shop.payload.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class DeviceFingerprintVerificationFilter extends OncePerRequestFilter {

    @Value("${security.device-fingerprint.block-unknown:true}") // Configurable policy: block unknown devices?
    private boolean blockUnknownDevices;

    @Value("${security.max-devices-per-user}")
    private int maxDevicesPerUser; // Keep this consistent with UserAuthenticator

    // List of paths to exclude from fingerprint verification (e.g., login, register, public assets)
    // Ensure this list is comprehensive and accurate for your API structure.
    private final List<String> excludedPaths = List.of(
            "/api/auth/", // Exclude all auth endpoints by prefix
            "/public/",
            "/error",
            "/actuator", // Exclude actuator endpoints (consider finer-grained control if needed)
            "/health"
            // Add other public paths as needed
    );

    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprintVerificationFilter.class);

    private final DeviceFingerprintService deviceFingerprintService;
    private final UserDeviceFingerprintRepository userDeviceFingerprintRepository;
    private final SecurityAuditService auditService;
    private final ObjectMapper objectMapper; // For writing error responses

    public DeviceFingerprintVerificationFilter(DeviceFingerprintService deviceFingerprintService,
                                               UserDeviceFingerprintRepository userDeviceFingerprintRepository,
                                               SecurityAuditService auditService,
                                               ObjectMapper objectMapper) {
        this.deviceFingerprintService = deviceFingerprintService;
        this.userDeviceFingerprintRepository = userDeviceFingerprintRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {

        // Skip verification for excluded paths
        String requestPath = request.getRequestURI();
        if (isExcludedPath(requestPath)) {
            log.trace("Skipping device fingerprint verification for excluded path: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        // Only proceed if a user is authenticated and the principal is our User object
        // This relies on the preceding authentication filter (e.g., PasetoAuthenticationFilter)
        // correctly setting the User entity as the principal.
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User user)) {
            log.trace("Skipping device fingerprint verification for unauthenticated request or invalid principal type for path: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Check if fingerprinting is enabled for the user
            if (!user.isDeviceFingerprintingEnabled()) {
                log.trace("Device fingerprinting disabled for user {}. Skipping verification.", user.getUserId());
                filterChain.doFilter(request, response);
                return;
            }

            // Generate fingerprint for the current request
            // Consider passing more headers/params if needed for robustness
            String currentFingerprint = deviceFingerprintService.generateDeviceFingerprint(
                    request.getHeader("User-Agent"),
                    request.getParameter("screenWidth"), // Ensure these are consistently sent by the client
                    request.getParameter("screenHeight"),
                    request
            );

            if (currentFingerprint == null) {
                log.warn("Could not generate device fingerprint for user {}. Blocking request.", user.getUserId());
                auditService.logFailedLogin(user.getUsername(), request.getRemoteAddr(), "Fingerprint Generation Failed");
                sendErrorResponse(response, HttpStatus.BAD_REQUEST, "DEVICE_FINGERPRINT_GENERATION_FAILED", "Could not determine device fingerprint.");
                return;
            }

            // Find known fingerprints for the user
            Optional<UserDeviceFingerprint> knownDeviceOpt = userDeviceFingerprintRepository
                    .findByUserAndDeviceFingerprint(user, currentFingerprint);

            if (knownDeviceOpt.isPresent()) {
                // --- Known Device Scenario ---
                UserDeviceFingerprint knownDevice = knownDeviceOpt.get();
                if (!knownDevice.isActive()) {
                    log.warn("Attempt to use inactive device fingerprint {} for user {}", currentFingerprint, user.getUserId());
                    auditService.logFailedLogin(user.getUsername(), request.getRemoteAddr(), "Inactive Device Fingerprint");
                    sendErrorResponse(response, HttpStatus.FORBIDDEN, "INACTIVE_DEVICE", "This device is no longer active for your account.");
                    return;
                }
                // If known and active, update usage details and proceed
                knownDevice.setLastUsedAt(Instant.now());
                knownDevice.setLastKnownIp(request.getRemoteAddr()); // Update IP on successful use
                knownDevice.setFailedAttempts(0); // Reset failed attempts on successful verification
                userDeviceFingerprintRepository.save(knownDevice);
                log.trace("Device fingerprint {} verified for user {}", currentFingerprint, user.getUserId());
                filterChain.doFilter(request, response);
            } else {
                // --- Unknown Device Scenario ---
                log.warn("Unknown device fingerprint {} detected for user {}", currentFingerprint, user.getUserId());
                auditService.logFailedLogin(user.getUsername(), request.getRemoteAddr(), "Unknown Device Fingerprint");

                // Policy: Block unknown devices if configured
                if (blockUnknownDevices) {
                    log.warn("Blocking access from unknown device {} for user {} due to policy.", currentFingerprint, user.getUserId());
                    sendErrorResponse(response, HttpStatus.FORBIDDEN, "UNKNOWN_DEVICE", "Access from this device is not permitted.");
                    // Optionally: Still attempt to record the device attempt, even if blocked
                    // try { handleUnknownDevice(user, currentFingerprint, request.getRemoteAddr()); } catch (MaxDevicesExceededException ignored) {}
                } else {
                    // Policy: Allow unknown devices but log/handle
                    log.info("Allowing access from unknown device {} for user {} (policy allows). Attempting to add device.", currentFingerprint, user.getUserId());
                    try {
                        // Attempt to add the device as untrusted
                        handleUnknownDevice(user, currentFingerprint, request.getRemoteAddr());
                        filterChain.doFilter(request, response); // Proceed after adding (or attempting to add)
                    } catch (MaxDevicesExceededException e) {
                        // If adding fails due to limit, even if policy allows unknown, we might need to block
                        log.warn("Max device limit reached for user {} while trying to add allowed unknown device {}. Blocking request.", user.getUserId(), currentFingerprint);
                        sendErrorResponse(response, HttpStatus.FORBIDDEN, "MAX_DEVICES_EXCEEDED", "Maximum device limit reached. Cannot use new device.");
                        // Do NOT proceed with filterChain.doFilter here
                    }
                }
            }

        } catch (DeviceFingerprintMismatchException | InactiveDeviceFingerprintException e) {
            // Specific exceptions related to fingerprint validation failures
            log.warn("Device fingerprint validation failed for user {}: {}", authentication.getPrincipal() instanceof User u ? u.getUserId() : "UNKNOWN", e.getMessage());
            sendErrorResponse(response, HttpStatus.FORBIDDEN, "DEVICE_VERIFICATION_FAILED", e.getMessage());
        } catch (Exception e) {
            // Catch-all for unexpected errors during the filter process
            log.error("Error during device fingerprint verification for user {}: {}", authentication.getPrincipal() instanceof User u ? u.getUserId() : "UNKNOWN", e.getMessage(), e);
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "FINGERPRINT_VERIFICATION_ERROR", "An internal error occurred during device verification.");
        }
    }

    private boolean isExcludedPath(String requestPath) {
        // Use startsWith for prefix matching
        return excludedPaths.stream().anyMatch(requestPath::startsWith);
    }

    // Helper to send a standardized JSON error response
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String errorCode, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message); // Use standard ErrorResponse record/class
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush(); // Ensure the response is sent
    }

    /**
     * Handles adding an unknown device fingerprint for a user.
     * Adds the device as active but untrusted if the user's device limit is not reached.
     *
     * @param user        The user associated with the device.
     * @param fingerprint The new device fingerprint string.
     * @param ipAddress   The IP address from which the request originated.
     * @throws MaxDevicesExceededException if the user has already reached the maximum allowed devices.
     */
    private void handleUnknownDevice(User user, String fingerprint, String ipAddress) throws MaxDevicesExceededException {
        long deviceCount = userDeviceFingerprintRepository.countByUserAndIsActiveTrue(user);
        if (deviceCount < maxDevicesPerUser) {
            UserDeviceFingerprint newDevice = UserDeviceFingerprint.builder()
                    .user(user)
                    .deviceFingerprint(fingerprint)
                    .lastKnownIp(ipAddress)
                    .lastUsedAt(Instant.now())
                    .isActive(true) // Add as active
                    .trusted(false) // Add as untrusted initially
                    .failedAttempts(0) // Start with 0 failed attempts
                    // .deviceName("Unknown Device") // Consider adding a default name or deriving it
                    // .browserInfo(deriveBrowserInfo(request)) // If possible
                    .build();
            userDeviceFingerprintRepository.save(newDevice);
            log.info("Added new untrusted device fingerprint {} for user {}", fingerprint, user.getUserId());
            // Optionally: Trigger notification to user about a new device being added
            // notificationService.sendNewDeviceNotification(user, newDevice);
        } else {
            log.warn("Max device limit ({}) reached for user {}. Cannot add new unknown device {}.", maxDevicesPerUser, user.getUserId(), fingerprint);
            // Throw exception to be caught by the caller
            throw new MaxDevicesExceededException("Maximum device limit reached, cannot add new device.");
        }
    }
}