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
    private int maxDevicesPerUser;

    // List of paths to exclude from fingerprint verification (e.g., login, register, public assets)
    private final List<String> excludedPaths = List.of(
            "/api/auth/login",
            "/api/auth/register",
//            "/api/auth/logout", // Logout might not need verification
            "/api/auth/v1/mfa/validate", // MFA validation itself
            "/public/",
            "/error",
            "/actuator", // Exclude actuator endpoints
            "/health"
            // Add other public paths as needed
    );

    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprintVerificationFilter.class);

    private final DeviceFingerprintService deviceFingerprintService;
    private final UserDeviceFingerprintRepository userDeviceFingerprintRepository;
    private final SecurityAuditService auditService;
    private final ObjectMapper objectMapper; // For writing error responses

    public DeviceFingerprintVerificationFilter(DeviceFingerprintService deviceFingerprintService, UserDeviceFingerprintRepository userDeviceFingerprintRepository, SecurityAuditService auditService, ObjectMapper objectMapper) {
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

        // Only proceed if user is authenticated and the principal is our User object
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User user)) {
            log.trace("Skipping device fingerprint verification for unauthenticated request or invalid principal type.");
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
            String currentFingerprint = deviceFingerprintService.generateDeviceFingerprint(
                    request.getHeader("User-Agent"),
                    request.getParameter("screenWidth"), // Assuming passed as param or header
                    request.getParameter("screenHeight"),// Assuming passed as param or header
                    request
            );

            if (currentFingerprint == null) {
                log.warn("Could not generate device fingerprint for user {}. Blocking request.", user.getUserId());
                sendErrorResponse(response, HttpStatus.BAD_REQUEST, "DEVICE_FINGERPRINT_GENERATION_FAILED", "Could not determine device fingerprint.");
                return;
            }

            // Find known fingerprints for the user
            Optional<UserDeviceFingerprint> knownDeviceOpt = userDeviceFingerprintRepository
                    .findByUserAndDeviceFingerprint(user, currentFingerprint);

            if (knownDeviceOpt.isPresent()) {
                UserDeviceFingerprint knownDevice = knownDeviceOpt.get();
                if (!knownDevice.isActive()) {
                    log.warn("Attempt to use inactive device fingerprint {} for user {}", currentFingerprint, user.getUserId());
                    auditService.logFailedLogin(user.getUsername(), request.getRemoteAddr(), "Inactive Device Fingerprint"); // Or a specific audit event
                    sendErrorResponse(response, HttpStatus.FORBIDDEN, "INACTIVE_DEVICE", "This device is no longer active.");
                    return;
                }
                // If known and active, update last used time and proceed
                knownDevice.setLastUsedAt(Instant.now());
                knownDevice.setLastKnownIp(request.getRemoteAddr()); // Update IP
                userDeviceFingerprintRepository.save(knownDevice);
                log.trace("Device fingerprint {} verified for user {}", currentFingerprint, user.getUserId());
                filterChain.doFilter(request, response);
            } else {
                // Fingerprint is unknown
                log.warn("Unknown device fingerprint {} detected for user {}", currentFingerprint, user.getUserId());
                auditService.logFailedLogin(user.getUsername(), request.getRemoteAddr(), "Unknown Device Fingerprint"); // Or a specific audit event

                // Policy: Block unknown devices if configured
                if (blockUnknownDevices) {
                    sendErrorResponse(response, HttpStatus.FORBIDDEN, "UNKNOWN_DEVICE", "Access from this device is not permitted.");
                    // Optionally: Check device limit and add as untrusted if limit not reached
                    // handleUnknownDevice(user, currentFingerprint, request.getRemoteAddr());
                } else {
                    // Policy: Allow unknown devices but log (or potentially require MFA step-up later)
                    log.info("Allowing access from unknown device {} for user {} (policy allows)", currentFingerprint, user.getUserId());
                    // Optionally add the device as untrusted here
                    // handleUnknownDevice(user, currentFingerprint, request.getRemoteAddr());
                    filterChain.doFilter(request, response);
                }
            }

        } catch (Exception e) {
            log.error("Error during device fingerprint verification for user {}", user.getUserId(), e);
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "FINGERPRINT_VERIFICATION_ERROR", "An internal error occurred during device verification.");
        }
    }

    private boolean isExcludedPath(String requestPath) {
        return excludedPaths.stream().anyMatch(requestPath::startsWith);
    }

    // Helper to send a standardized JSON error response
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String errorCode, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message); // Use our ErrorResponse record/class
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    // Optional: Helper to handle adding an unknown device (e.g., as untrusted)
    private void handleUnknownDevice(User user, String fingerprint, String ipAddress) {
        long deviceCount = userDeviceFingerprintRepository.countByUserAndIsActiveTrue(user);
        if (deviceCount < maxDevicesPerUser) {
            UserDeviceFingerprint newDevice = UserDeviceFingerprint.builder()
                    .user(user)
                    .deviceFingerprint(fingerprint)
                    .lastKnownIp(ipAddress)
                    .lastUsedAt(Instant.now())
                    .isActive(true) // Add as active
                    .trusted(false) // Add as untrusted initially
                    .build();
            userDeviceFingerprintRepository.save(newDevice);
            log.info("Added new untrusted device fingerprint {} for user {}", fingerprint, user.getUserId());
            // Optionally: Trigger notification to user about a new device
        } else {
            log.warn("Max device limit reached for user {}. Cannot add new unknown device {}.", user.getUserId(), fingerprint);
            // Policy might still block here even if blockUnknownDevices is false, due to limit.
            // Throw MaxDevicesExceededException or handle as per requirements.
            throw new MaxDevicesExceededException("Maximum device limit reached, cannot add new device.");
        }
    }
}