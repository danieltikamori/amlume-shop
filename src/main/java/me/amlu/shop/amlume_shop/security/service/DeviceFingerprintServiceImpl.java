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

import org.apache.commons.lang3.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import me.amlu.shop.amlume_shop.config.properties.SecurityProperties;
import me.amlu.shop.amlume_shop.exceptions.*;
import me.amlu.shop.amlume_shop.model.User;
import me.amlu.shop.amlume_shop.model.UserDeviceFingerprint;
import me.amlu.shop.amlume_shop.repositories.UserDeviceFingerprintRepository;
import me.amlu.shop.amlume_shop.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;

import static me.amlu.shop.amlume_shop.commons.Constants.*;

@Slf4j
@Service
@Transactional
public class DeviceFingerprintServiceImpl implements DeviceFingerprintService {

    private final UserRepository userRepository;
    private final UserDeviceFingerprintRepository userDeviceFingerprintRepository;
    private final AuditLogger auditLogger;
    private final SecurityProperties securityProperties;
    private final IpValidationService ipValidationService;
    private final IpSecurityService ipSecurityService;
    private final IpRateLimitService ipRateLimitService;

    private final String fingerprintSalt;
    private final int maxDevicesPerUser;

    public DeviceFingerprintServiceImpl(@Value("${FINGERPRINT_SALT}") String fingerprintSalt, @Value("${security.max-devices-per-user}") int maxDevicesPerUser, UserRepository userRepository, UserDeviceFingerprintRepository userDeviceFingerprintRepository, AuditLogger auditLogger, SecurityProperties securityProperties, IpValidationService ipValidationService, IpSecurityService ipSecurityService, IpRateLimitService ipRateLimitService) {
        this.fingerprintSalt = Objects.requireNonNull(fingerprintSalt, "Fingerprint salt cannot be null");
        this.maxDevicesPerUser = maxDevicesPerUser;
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
        this.userDeviceFingerprintRepository = Objects.requireNonNull(userDeviceFingerprintRepository, "UserDeviceFingerprintRepository cannot be null");
        this.auditLogger = Objects.requireNonNull(auditLogger, "AuditLogger cannot be null");
        this.securityProperties = Objects.requireNonNull(securityProperties, "SecurityProperties cannot be null");
        this.ipValidationService = Objects.requireNonNull(ipValidationService, "IpValidationService cannot be null");
        this.ipSecurityService = Objects.requireNonNull(ipSecurityService, "IpSecurityService cannot be null");
        this.ipRateLimitService = Objects.requireNonNull(ipRateLimitService, "IpRateLimitService cannot be null");
    }

    @Override
    public void registerDeviceFingerprint(String userId, String userAgent, String screenWidth, String screenHeight, HttpServletRequest request) throws DeviceFingerprintRegistrationException {
        validateInputs(userId, request);
        String clientIp = getClientIpAddress(request);

        // Validate IP and spoofing
        ipSecurityCheck(request, clientIp);

        // Apply rate limiting
        applyRateLimitingOnRegisterDevice(userId, clientIp);

        logIpAddressResolution(userId, clientIp, request.getHeader(X_FORWARDED_FOR));

        try {
            User user = findAndValidateUser(userId);

            // Check if user has disabled device fingerprinting
            validateDeviceFingerprinting(user);

            // Check if user has reached maximum devices
            validateDeviceLimit(user);

            String fingerprint = generateDeviceFingerprint(userAgent, screenWidth, screenHeight, request);

            processFingerprint(user, fingerprint, clientIp);

        } catch (Exception e) {
            handleRegistrationError(userId, e);
        }
    }

    private void ipSecurityCheck(HttpServletRequest request, String clientIp) {
        // Validate IP
        if (!ipValidationService.isValidIp(clientIp)) {
            log.warn("Invalid IP detected: {}", clientIp);
        }

        // Check for spoofing
        if (ipSecurityService.isIpSuspicious(clientIp, request)) {
            log.warn("Suspicious IP detected: {}", clientIp);
            throw new SecurityException("Suspicious IP activity detected");
        }
    }

    private void applyRateLimitingOnRegisterDevice(String userId, String clientIp) {
        ipRateLimitService.checkRateLimit(clientIp).thenAccept(allowed -> {
            if (!allowed) {
                log.warn("Rate limit exceeded for IP: {}", clientIp);
                auditLogger.logSecurityEvent("RATE_LIMIT_EXCEEDED", userId,"No yet generated fingerprint");
                throw new RateLimitExceededException("Rate limit exceeded for IP address");
            }
                });
    }

    private void validateInputs(String userId, HttpServletRequest request) {
        if (StringUtils.isBlank(userId)) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
        Objects.requireNonNull(request, "Request cannot be null");
    }

    private User findAndValidateUser(String userId) {
        return userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));
    }

    private void validateDeviceFingerprinting(User user) {
        if (!user.isDeviceFingerprintingEnabled()) {
            log.info("Device fingerprinting is disabled for user: {}", user.getUserId());
            throw new DeviceFingerprintingDisabledException("Device fingerprinting is disabled");
        }
    }

    private void validateDeviceLimit(User user) {
        long deviceCount = userDeviceFingerprintRepository.countByUser(user);
        if (deviceCount >= maxDevicesPerUser) {
            log.warn("Maximum device limit reached for user: {}", user.getUserId());
            throw new MaxDevicesExceededException("Maximum number of devices reached for user");
        }
    }

    private void processFingerprint(User user, String deviceFingerprint, String clientIp) {
        userDeviceFingerprintRepository
                .findByUserAndDeviceFingerprint(user, deviceFingerprint)
                .ifPresentOrElse(
                        this::updateExistingFingerprint,
                        () -> createNewFingerprint(user, deviceFingerprint, clientIp)
                );
    }

    private void updateExistingFingerprint(UserDeviceFingerprint existing) {
        existing.setLastUsedAt(Instant.now());
        userDeviceFingerprintRepository.save(existing);
        log.debug("Updated last used timestamp for existing device fingerprint: {}",
                existing.getDeviceFingerprint());
    }

    private void createNewFingerprint(User user, String fingerprint, String clientIp) {
        UserDeviceFingerprint newFingerprint = UserDeviceFingerprint.builder()
                .user(user)
                .deviceFingerprint(fingerprint)
                .lastUsedAt(Instant.now())
                .lastKnownIp(clientIp)
                .isActive(true)
                .build();
        userDeviceFingerprintRepository.save(newFingerprint);
        log.info("New device fingerprint registered for user: {}", user.getUserId());
    }

    private void handleRegistrationError(String userId, Exception e) throws DeviceFingerprintRegistrationException {
        log.error("Error registering device fingerprint for user: {}", userId, e);
        if (e instanceof DeviceFingerprintingDisabledException ||
                e instanceof MaxDevicesExceededException) {
            throw new DeviceFingerprintRegistrationException(e.getMessage());
        }
        throw new DeviceFingerprintAdditionException("Failed to register device fingerprint", e);
    }

    @Override
    public void validateDeviceFingerprint(String userId, String deviceFingerprint, HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);

        // Validate IP and spoofing
        ipSecurityCheck(request, clientIp);

        try {
            User user = findAndValidateUser(userId);
            UserDeviceFingerprint fingerprint = findAndValidateFingerprint(user, deviceFingerprint);

            validateIpSecurity(userId, clientIp, fingerprint);
            updateFingerprintUsage(fingerprint, clientIp);

            auditLogger.logDeviceValidation(user, fingerprint, true);

        } catch (Exception e) {
            handleValidationError(userId, deviceFingerprint, clientIp, e);
        }
    }

    @Override
    public void deleteDeviceFingerprint(String userId, String deviceFingerprint) {
        try {
            User user = findAndValidateUser(userId);
            UserDeviceFingerprint fingerprint = findAndValidateFingerprint(user, deviceFingerprint);

            userDeviceFingerprintRepository.delete(fingerprint);
            log.info("Device fingerprint {} deleted for user {}", deviceFingerprint, userId);
            auditLogger.logDeviceDeletion(user, fingerprint);

        } catch (Exception e) {
            handleDeletionError(userId, deviceFingerprint, e);
        }
    }

    @Override
    public void deleteDeviceFingerprint(User user, Long fingerprintId) {
        try {
            UserDeviceFingerprint fingerprint = userDeviceFingerprintRepository.findById(fingerprintId) // Find by ID
                    .orElseThrow(() -> new DeviceFingerprintNotFoundException("Device Fingerprint not found"));

            userDeviceFingerprintRepository.delete(fingerprint); // Delete the entity
            log.info("Device fingerprint {} deleted for user {}", fingerprintId, user.getUserId());

        } catch (DeviceFingerprintNotFoundException e) {
            log.error("Error deleting fingerprint", e);
            throw e; // Re-throw the exception after logging
        }
    }

    @Override
    public String generateDeviceFingerprint(String userAgent, String screenWidth, String screenHeight, HttpServletRequest request) {
        try {
            Map<String, String> data = collectDeviceData(userAgent, screenWidth, screenHeight, request);

            // Browser capabilities and security headers
            addBrowserHeaders(data, request);
            addSecurityHeaders(data, request);

            // Platform-specific information
            addPlatformInfo(data, request);

            if (data.isEmpty()) {
                log.warn("No valid device data collected for fingerprinting");
                return generateFallbackFingerprint();
            }

            return generateFingerprintHash(data);

        } catch (Exception e) {
            log.error("Error generating device fingerprint", e);
            return generateFallbackFingerprint();
        }
    }

    private Map<String, String> collectDeviceData(String userAgent, String screenWidth,
                                                  String screenHeight, HttpServletRequest request) {
        Map<String, String> data = new LinkedHashMap<>(INITIAL_MAP_CAPACITY);

        addDataIfValid(data, USER_AGENT, userAgent);
        addDataIfValid(data, "Screen-Width", screenWidth);
        addDataIfValid(data, "Screen-Height", screenHeight);
        addDataIfValid(data, ACCEPT_LANGUAGE, request.getHeader(ACCEPT_LANGUAGE));
        addDataIfValid(data, "Platform", getPlatform(userAgent));
        addDataIfValid(data, "Time-Zone", getTimeZone(request));
        addDataIfValid(data, "IP-Address", getClientIpAddress(request));
        addSessionData(data, request);

        return data;
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

    private void addBrowserHeaders(Map<String, String> data, HttpServletRequest request) {
        addDataIfValid(data, "Accept", request.getHeader("Accept"));
        addDataIfValid(data, "Accept-Encoding", request.getHeader("Accept-Encoding"));
        addDataIfValid(data, "Connection", request.getHeader("Connection"));
        addDataIfValid(data, "Host", request.getHeader("Host"));
    }

    private void addSecurityHeaders(Map<String, String> data, HttpServletRequest request) {
        addDataIfValid(data, "Sec-Fetch-Site", request.getHeader("Sec-Fetch-Site"));
        addDataIfValid(data, "Sec-Fetch-Mode", request.getHeader("Sec-Fetch-Mode"));
        addDataIfValid(data, "Sec-Fetch-User", request.getHeader("Sec-Fetch-User"));
        addDataIfValid(data, "Sec-Fetch-Dest", request.getHeader("Sec-Fetch-Dest"));
        addDataIfValid(data, "Sec-Ch-Ua", request.getHeader("Sec-Ch-Ua"));
        addDataIfValid(data, "Sec-Ch-Ua-Mobile", request.getHeader("Sec-Ch-Ua-Mobile"));
        addDataIfValid(data, "Sec-Ch-Ua-Platform", request.getHeader("Sec-Ch-Ua-Platform"));
    }

    private void addPlatformInfo(Map<String, String> data, HttpServletRequest request) {
        // Add any custom headers your frontend sends
        addDataIfValid(data, "Platform-Type", request.getHeader("X-Platform-Type"));
        addDataIfValid(data, "App-Version", request.getHeader("X-App-Version"));
        addDataIfValid(data, "Device-Model", request.getHeader("X-Device-Model"));
    }

    private void addSessionData(Map<String, String> data, HttpServletRequest request) {
        Optional.ofNullable(request.getSession(false))
                .map(HttpSession::getId)
                .ifPresent(sessionId -> addDataIfValid(data, "Session-ID", sessionId));
    }

    private String generateFingerprintHash(Map<String, String> data) {
        try {
            TreeMap<String, String> sortedData = new TreeMap<>(data);
            StringBuilder rawFingerprint = new StringBuilder(buildRawFingerprint(sortedData));

            // Add timestamp-based component for uniqueness
            String timeComponent = String.valueOf(System.currentTimeMillis() / (24 * 60 * 60 * 1000)); // Daily rotation
            rawFingerprint.append(timeComponent);

            return hashWithSalt(rawFingerprint.toString());
        } catch (NoSuchAlgorithmException e) {
            log.error("Hash algorithm not available", e);
            return generateFallbackFingerprint();
        }
    }

    private String buildRawFingerprint(TreeMap<String, String> sortedData) {
        return sortedData.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining("|"));
    }

    private String hashWithSalt(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] saltedInput = (input + fingerprintSalt).getBytes(StandardCharsets.UTF_8);
        byte[] hash = digest.digest(saltedInput);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private UserDeviceFingerprint findAndValidateFingerprint(User user, String fingerprintId) {
        if (user == null || StringUtils.isBlank(fingerprintId)) {
            log.error("Invalid input parameters for fingerprint validation");
            throw new IllegalArgumentException("User and fingerprint ID must not be null or empty");
        }

        try {
            UserDeviceFingerprint fingerprint = userDeviceFingerprintRepository
                    .findByUserAndDeviceFingerprint(user, fingerprintId)
                    .orElseThrow(() -> new DeviceFingerprintNotFoundException(
                            String.format("Device Fingerprint not found for user %s", user.getUserId())));

            if (!fingerprint.isActive()) {
                log.warn("Attempt to use inactive fingerprint: {} for user: {}", fingerprintId, user.getUserId());
                throw new InactiveDeviceFingerprintException("Device fingerprint is inactive");
            }

            return fingerprint;
        } catch (Exception e) {
            log.error("Error validating fingerprint: {} for user: {}", fingerprintId, user.getUserId(), e);
            throw new DeviceFingerprintValidationException("Error validating device fingerprint", e);
        }
    }

    private void validateIpSecurity(String userId, String clientIp, UserDeviceFingerprint fingerprint) {
        if (StringUtils.isBlank(clientIp)) {
            log.error("Empty or null client IP address");
            throw new InvalidIpAddressException("Client IP address cannot be empty");
        }

        try {
            // Validate IP format
            if (!ipValidationService.isValidIp(clientIp)) {
                log.warn("Invalid IP address detected: {}", clientIp);
                auditLogger.logSecurityEvent("INVALID_IP_ATTEMPT", userId, String.valueOf(fingerprint.getUserDeviceFingerprintId()), clientIp);
                throw new InvalidIpAddressException("Invalid IP address format");
            }

            // Check if IP is blocked
            if (ipSecurityService.isIpBlocked(clientIp)) {
                log.warn("Blocked IP address attempted access: {}", clientIp);
                auditLogger.logSecurityEvent("BLOCKED_IP_ATTEMPT", userId, String.valueOf(fingerprint.getUser().getUserId()), clientIp);
                throw new BlockedIpAddressException("IP address is blocked");
            }

            // Check rate limiting
            ipRateLimitService.checkRateLimit(clientIp).thenAccept(allowed -> {
                if (!allowed) {
                    log.warn("Rate limit exceeded for IP: {}", clientIp);
                    auditLogger.logSecurityEvent("RATE_LIMIT_EXCEEDED", userId, String.valueOf(fingerprint.getUser().getUserId()), clientIp);
                    throw new RateLimitExceededException("Rate limit exceeded for IP address");
                }
            });

            // Log successful validation
            log.debug("IP security validation passed for IP: {}", clientIp);

        } catch (Exception e) {
            if (!(e instanceof InvalidIpAddressException ||
                    e instanceof BlockedIpAddressException ||
                    e instanceof RateLimitExceededException)) {
                log.error("Unexpected error during IP security validation", e);
                throw new IpValidationException("Error during IP security validation", e);
            }

        }
    }

    private void updateFingerprintUsage(UserDeviceFingerprint fingerprint, String clientIp) {
        if (fingerprint == null) {
            throw new IllegalArgumentException("Fingerprint cannot be null");
        }

        try {
            Instant now = Instant.now();

            // Update fingerprint details
            fingerprint.setLastUsedAt(now);
            fingerprint.setLastKnownIp(clientIp);
            fingerprint.setUpdateCount(fingerprint.getUpdateCount() + 1);

            // Save and verify the update
            UserDeviceFingerprint updatedFingerprint = userDeviceFingerprintRepository.save(fingerprint);

            if (!updatedFingerprint.getLastUsedAt().equals(now)) {
                log.warn("Fingerprint update verification failed for user: {}", fingerprint.getUser().getUserId());
                throw new DeviceFingerprintUpdateException("Failed to update fingerprint usage");
            }

            log.debug("Successfully updated fingerprint usage for user: {}", fingerprint.getUser().getUserId());

            // Log the update for audit purposes
            auditLogger.logFingerprintUpdate(fingerprint.getUser().getUserId(), clientIp);

        } catch (Exception e) {
            log.error("Error updating fingerprint usage for user: {}",
                    fingerprint.getUser().getUserId(), e);
            throw new DeviceFingerprintUpdateException("Failed to update fingerprint usage", e);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {

        return Arrays.stream(IP_HEADERS)
                .map(request::getHeader)
                .filter(this::isValidIpHeader)
                .map(this::extractFirstIp)
                .findFirst()
                .orElseGet(request::getRemoteAddr);
    }

    private boolean isValidIpHeader(String ip) {
        return StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip);
    }

    private String extractFirstIp(String ip) {
        return ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    /**
     * Gets the timezone from the request header.
     * If the header is not present or empty, the default timezone of UTC is used.
     *
     * @param request The HTTP request
     * @return The timezone as a string
     */
    private String getTimeZone(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Time-Zone"))
                .orElse(ZoneOffset.UTC.getId());
    }

    private void addDataIfValid(Map<String, String> data, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            data.put(key, value);
        }
    }

    private String generateFallbackFingerprint() {
        return "fallbackFingerprint_" + UUID.randomUUID().toString();
//        return UUID.randomUUID().toString();
    }

    private void handleValidationError(String userId, String fingerprintId, String clientIp, Exception e) {
        log.error("Error validating device fingerprint for user: {} fingerprint: {}", userId, fingerprintId, e);
        auditLogger.logFailedValidation(userId, fingerprintId, clientIp);
        throw new DeviceFingerprintValidationException("Failed to validate device fingerprint", e);
    }

    private void handleDeletionError(String userId, String fingerprintId, Exception e) {
        log.error("Error deleting device fingerprint for user: {} fingerprint: {}", userId, fingerprintId, e);
        throw new DeviceFingerprintDeletionException("Failed to delete device fingerprint", e);
    }

    @Override
    public void verifyDeviceFingerprint(String tokenFingerprint, HttpServletRequest request, String userId) throws DeviceFingerprintMismatchException, UserNotFoundException {
        String currentFingerprint = generateDeviceFingerprint(
                request.getHeader("User-Agent"),
                request.getHeader("Screen-Width"),
                request.getHeader("Screen-Height"),
                request);

        // Early validation
        if (tokenFingerprint == null || currentFingerprint == null) {
            log.warn("Missing fingerprint data for user: {}", userId);
            throw new DeviceFingerprintMismatchException("Invalid fingerprint data");
        }

        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        // Check if fingerprint matches token
        if (!tokenFingerprint.equals(currentFingerprint)) {
            // Check if it's a known device
            List<UserDeviceFingerprint> fingerprints = userDeviceFingerprintRepository.findByUser(user);

            boolean isKnownDevice = fingerprints != null &&
                    fingerprints.stream()
                            .anyMatch(fp -> fp.getDeviceFingerprint().equals(currentFingerprint) && fp.isActive());

            if (!isKnownDevice) {
                log.warn("Unrecognized device for user: {}", userId);
                throw new DeviceFingerprintMismatchException("Unrecognized device");
            }

            // Update last used timestamp for the matching device
            fingerprints.stream()
                    .filter(fp -> fp.getDeviceFingerprint().equals(currentFingerprint))
                    .findFirst()
                    .ifPresent(this::updateExistingFingerprint);
        }

        log.info("Device fingerprint verified for user: {}", userId);
    }

    private boolean isValidFingerprintFormat(String fingerprint) {
        // Implement your fingerprint validation logic
        // For example, checking length, allowed characters, etc.
        return fingerprint.matches("^[a-zA-Z0-9-_]{32,256}$");
    }

    @Override
    public void trustDevice(long userId, String deviceFingerprint) {
        userDeviceFingerprintRepository.findByUserIdAndFingerprint(userId, deviceFingerprint)
                .ifPresent(device -> {
                    device.setTrusted(true);
                    device.setLastUsedAt(Instant.now());
                    userDeviceFingerprintRepository.save(device);
                });
    }


    @Override
    public void storeOrUpdateFingerprint(User user, String accessToken, String refreshToken, String deviceFingerprint) {
        // Input validation
        validateInput(user, deviceFingerprint);

        userDeviceFingerprintRepository.findByUserAndDeviceFingerprint(user, deviceFingerprint)
                .ifPresentOrElse(
                        existing -> updateExistingFingerprint(existing, accessToken, refreshToken),
                        () -> createNewFingerprint(user, accessToken, refreshToken, deviceFingerprint)
                );
    }

    @Override
    public void updateExistingFingerprint(UserDeviceFingerprint fingerprint,
                                          String accessToken,
                                          String refreshToken) {
        fingerprint.setLastUsedAt(Instant.now());
        fingerprint.setAccessToken(accessToken);
        fingerprint.setRefreshToken(refreshToken);
        fingerprint.setFailedAttempts(0); // Reset failed attempts on successful update
        userDeviceFingerprintRepository.save(fingerprint);

        auditLogger.logDeviceAccess(String.valueOf(fingerprint.getUser().getUserId()),
                fingerprint.getDeviceFingerprint(),
                "DEVICE_UPDATED");
    }

    @Override
    public void createNewFingerprint(User user,
                                     String accessToken,
                                     String refreshToken,
                                     String deviceFingerprint) {
        // Check device limit
        checkDeviceLimit(user);

        UserDeviceFingerprint newFingerprint = UserDeviceFingerprint.builder()
                .user(user)
                .deviceFingerprint(deviceFingerprint)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
//                .createdAt(Instant.now()) // Already set in @PrePersist
//                .updatedAt(Instant.now())
                .lastUsedAt(Instant.now())
                .isActive(true)
                .trusted(false)
                .failedAttempts(0)
                .trusted(false)
                .build();

        userDeviceFingerprintRepository.save(newFingerprint);

        auditLogger.logDeviceAccess(String.valueOf(user.getUserId()),
                deviceFingerprint,
                "NEW_DEVICE_REGISTERED");
    }




    private void logIpAddressResolution(String userId, String resolvedIp, String originalHeader) {
        if (log.isDebugEnabled()) {
            log.debug("IP Address resolved for user {}: {} (from header: {})",
                    userId, resolvedIp, originalHeader);
        }
    }

    @Override
    public void markDeviceSuspicious(String userId, String deviceFingerprint) {
        userDeviceFingerprintRepository.findByUserIdAndFingerprint(Long.valueOf(userId), deviceFingerprint)
                .ifPresent(device -> {
                    device.setFailedAttempts(device.getFailedAttempts() + 1);
                    if (device.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                        device.setActive(false);
                        device.setDeactivatedAt(Instant.now());
                        auditLogger.logSecurityEvent("DEVICE_DEACTIVATED", userId, deviceFingerprint);
                    }
                    userDeviceFingerprintRepository.save(device);
                });
    }

    @Override
    public void revokeAllDevices(String userId, String exceptFingerprint) {
        List<UserDeviceFingerprint> devices = userDeviceFingerprintRepository
                .findByUserIdAndDeviceFingerprintNot(userId, exceptFingerprint); // TOCHECK: method

        devices.forEach(device -> {
            device.setActive(false);
            device.setDeactivatedAt(Instant.now());
            userDeviceFingerprintRepository.save(device);
        });

        auditLogger.logSecurityEvent("ALL_DEVICES_REVOKED EXCEPT this fingerprint:", userId, exceptFingerprint);
    }


    @Override
    public void checkDeviceLimit(User user) {
        long deviceCount = userDeviceFingerprintRepository.countByUser(user);
        int maxDevices = securityProperties.getMaxDevicesPerUser();

        if (deviceCount >= maxDevices) {
            String errorMessage = String.format(
                    "Maximum device limit (%d) reached for user: %s",
                    maxDevices,
                    user.getUserId()
            );
            log.warn(errorMessage);
            throw new TooManyDevicesException(errorMessage);
        }
    }

    @Override
    public void validateInput(User user, String deviceFingerprint) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (deviceFingerprint == null || deviceFingerprint.trim().isEmpty()) {
            throw new IllegalArgumentException("Device fingerprint must not be null or empty");
        }
        if (!isValidFingerprintFormat(deviceFingerprint)) {
            throw new IllegalArgumentException("Invalid fingerprint format");
        }
    }

    @Override
    public boolean verifyDevice(User user, String fingerprint) {
        return userDeviceFingerprintRepository.existsByUserAndFingerprint(user, fingerprint);
    }

    @Override
    public void deactivateDevice(String userId, String deviceFingerprint) {
        userDeviceFingerprintRepository.findByUserIdAndFingerprint(Long.valueOf(userId), deviceFingerprint)
                .ifPresent(device -> {
                    device.setActive(false);
                    device.setDeactivatedAt(Instant.now());
                    userDeviceFingerprintRepository.save(device);
                });
    }

    @Override
    public void disableDeviceFingerprinting(User user) {
        user.setDeviceFingerprintingEnabled(false);
        userDeviceFingerprintRepository.findByUser(user)
                .forEach(device -> {
                    device.setActive(false);
                    device.setDeactivatedAt(Instant.now());
                    userDeviceFingerprintRepository.save(device);
                });
        userRepository.save(user);
        auditLogger.logSecurityEvent("DEVICE_FINGERPRINTING_DISABLED", String.valueOf(user.getUserId()), null);
        log.warn("Device fingerprinting disabled for user: {}", user.getUserId());
    }

    @Override
    public void enableDeviceFingerprinting(User user) {
        user.setDeviceFingerprintingEnabled(true);
        userRepository.save(user);
        auditLogger.logSecurityEvent("DEVICE_FINGERPRINTING_ENABLED", String.valueOf(user.getUserId()), null);
        log.warn("Device fingerprinting enabled for user: {}", user.getUserId());
    }
}