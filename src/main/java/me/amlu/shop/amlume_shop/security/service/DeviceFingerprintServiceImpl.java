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

    private void processFingerprint(User user, String fingerprint, String clientIp) {
        userDeviceFingerprintRepository
                .findByUserAndDeviceFingerprint(user, fingerprint)
                .ifPresentOrElse(
                        this::updateExistingFingerprint,
                        () -> createNewFingerprint(user, fingerprint, clientIp)
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
    public void deleteDeviceFingerprint(String userId, Long fingerprintId) {
        try {
            User user = userRepository.findById(Long.valueOf(userId))
                    .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

            UserDeviceFingerprint fingerprint = userDeviceFingerprintRepository.findById(fingerprintId)
                    .orElseThrow(() -> new DeviceFingerprintNotFoundException("Device Fingerprint not found"));

            // Security check - ensure fingerprint belongs to user
            if (!fingerprint.getUser().getUserId().equals(user.getUserId())) {
                log.warn("Attempted unauthorized fingerprint deletion. User: {}, Fingerprint: {}", userId, fingerprintId);
                throw new UnauthorizedAccessException("Unauthorized access to device fingerprint");
            }

            userDeviceFingerprintRepository.delete(fingerprint);
            log.info("Device fingerprint {} deleted for user {}", fingerprintId, userId);
        } catch (Exception e) {
            log.error("Error deleting fingerprint {} for user {}", fingerprintId, userId, e);
            throw e;
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
//            // Initialize map with known capacity to avoid resizing
//            Map<String, String> data = new LinkedHashMap<>(INITIAL_MAP_CAPACITY);
//
//            // Collect user agent once to avoid multiple requests
//            String userAgent = request.getHeader(USER_AGENT);

            Map<String, String> data = new TreeMap<>(); // Use TreeMap for automatic sorting

            // Add data points (handle nulls and empty values). Recommendation: Keep alphabetically ordered by key and consistent formatting.
            addDataPoints(screenWidth, screenHeight, request, data);

            // Network information with improved IP handling
            String ipAddress = getClientIpAddress(request);
            addData(data, "IP-Address", ipAddress);

            // Browser capabilities and security headers
            addBrowserHeaders(data, request);
            addSecurityHeaders(data, request);

            // Platform-specific information
            addPlatformInfo(data, request);

            // Validate collected data
            if (data.isEmpty()) {
                log.warn("No fingerprint data collected");
//                return UUID.randomUUID().toString(); // Fallback if no data
                return null; // Or handle as appropriate
            }

            // Add salt and hash
            // Generate fingerprint
            String saltedFingerprint = generateFingerprintHash(data);
            log.info("Generated device fingerprint: {}", saltedFingerprint);
            return saltedFingerprint;

        } catch (DeviceFingerprintGenerationException e) {
            log.error("Error generating fingerprint", e);
//            return UUID.randomUUID().toString(); // Fallback if SHA-256 is not available
            return generateFallbackFingerprint(); // Or handle as appropriate
        }
    }

    private void addDataPoints(String screenWidth, String screenHeight, HttpServletRequest request, Map<String, String> data) {
        addData(data, USER_AGENT, request.getHeader(USER_AGENT));
        addData(data, ACCEPT_LANGUAGE, request.getHeader(ACCEPT_LANGUAGE));
        addData(data, "Platform", getPlatform(request.getHeader(USER_AGENT)));
        addData(data, "Time-Zone", getTimeZone(request));
//            addData(data, "Time-Zone", request.getHeader("X-Time-Zone"));
        addData(data, "Screen-Width", screenWidth);
        addData(data, "Screen-Height", screenHeight);
        addData(data, "Accept-Language", request.getHeader("Accept-Language"));
        addData(data, "IP-address", request.getRemoteAddr());
        addData(data, "Session-ID", request.getSession().getId());
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


    private void addBrowserHeaders(Map<String, String> data, HttpServletRequest request) {
        addData(data, "Accept", request.getHeader("Accept"));
        addData(data, "Accept-Encoding", request.getHeader("Accept-Encoding"));
        addData(data, "Connection", request.getHeader("Connection"));
        addData(data, "Host", request.getHeader("Host"));
    }

    private void addSecurityHeaders(Map<String, String> data, HttpServletRequest request) {
        addData(data, "Sec-Fetch-Site", request.getHeader("Sec-Fetch-Site"));
        addData(data, "Sec-Fetch-Mode", request.getHeader("Sec-Fetch-Mode"));
        addData(data, "Sec-Fetch-User", request.getHeader("Sec-Fetch-User"));
        addData(data, "Sec-Fetch-Dest", request.getHeader("Sec-Fetch-Dest"));
        addData(data, "Sec-Ch-Ua", request.getHeader("Sec-Ch-Ua"));
        addData(data, "Sec-Ch-Ua-Mobile", request.getHeader("Sec-Ch-Ua-Mobile"));
        addData(data, "Sec-Ch-Ua-Platform", request.getHeader("Sec-Ch-Ua-Platform"));
    }

    private void addPlatformInfo(Map<String, String> data, HttpServletRequest request) {
        // Add any custom headers your frontend sends
        addData(data, "Platform-Type", request.getHeader("X-Platform-Type"));
        addData(data, "App-Version", request.getHeader("X-App-Version"));
        addData(data, "Device-Model", request.getHeader("X-Device-Model"));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = extractIpFromHeaders(request);

        // Validate IP
        if (!ipValidationService.isValidIp(ip)) {
            log.warn("Invalid IP detected: {}", ip);
            return "0.0.0.0";
        }

        // Check for spoofing
        if (ipSecurityService.isIpSuspicious(ip, request)) {
            log.warn("Suspicious IP detected: {}", ip);
            throw new SecurityException("Suspicious IP activity detected");
        }

        // Apply rate limiting
        ipRateLimitService.checkRateLimit(ip)
                .thenAccept(allowed -> {
                    if (!allowed) {
                        throw new RateLimitExceededException("Rate limit exceeded for IP: " + ip);
                    }
                });

        return ip;
    }

    private String extractIpFromHeaders(HttpServletRequest request) {
        String[] headerNamesToTry = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };


        for (String header : headerNamesToTry) {
            String ip = request.getHeader(header);
            if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // Handle multiple IP addresses
//                if (ip.contains(",")) {
//                    // Get first IP in the list (client IP)
//                    ip = ip.split(",")[0].trim();
//                }
//                    return ip;
                // Return first non-proxy IP if multiple IPs are present
                return ip.contains(",") ? ip.split(",")[0].trim() : ip;
                }
            }
        // Fallback to remote address
        return request.getRemoteAddr();
        }

    private String generateFallbackFingerprint() {
        return UUID.randomUUID().toString();
    }

    private String generateFingerprintHash(Map<String, String> data) {
        try {
            StringBuilder rawFingerprint = new StringBuilder();

            // Create normalized string from data
            for (Map.Entry<String, String> entry : data.entrySet()) {
                rawFingerprint.append(entry.getKey())
                        .append(":")
                        .append(entry.getValue())
                        .append("|");
            }

            // Add timestamp-based component for uniqueness
            String timeComponent = String.valueOf(System.currentTimeMillis() / (24 * 60 * 60 * 1000)); // Daily rotation
            rawFingerprint.append(timeComponent);

            // Add salt and generate hash
            return generateHashWithSalt(rawFingerprint);

        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating fingerprint hash", e);
            throw new DeviceFingerprintGenerationException("Failed to generate fingerprint hash", e);
        }
    }

    private String generateHashWithSalt(StringBuilder rawFingerprint) throws NoSuchAlgorithmException {
        String saltedFingerprint = rawFingerprint.toString() + fingerprintSalt;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(saltedFingerprint.getBytes(StandardCharsets.UTF_8));

        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
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
                    .ifPresent(this::updateDeviceLastUsed);
        }

        log.info("Device fingerprint verified for user: {}", userId);
    }

    private void updateDeviceLastUsed(UserDeviceFingerprint device) {
        device.setLastUsedAt(Instant.now());
        userDeviceFingerprintRepository.save(device);
    }

    private boolean isValidFingerprintFormat(String fingerprint) {
        // Implement your fingerprint validation logic
        // For example, checking length, allowed characters, etc.
        return fingerprint.matches("^[a-zA-Z0-9-_]{32,256}$");
    }

    @Override
    public void trustDevice(long userId, String fingerprint) {
        userDeviceFingerprintRepository.findByUserIdAndFingerprint(userId, fingerprint)
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
    public void markDeviceSuspicious(String userId, String fingerprint) {
        userDeviceFingerprintRepository.findByUserIdAndFingerprint(Long.valueOf(userId), fingerprint)
                .ifPresent(device -> {
                    device.setFailedAttempts(device.getFailedAttempts() + 1);
                    if (device.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                        device.setActive(false);
                        device.setDeactivatedAt(Instant.now());
                        auditLogger.logSecurityEvent(userId, fingerprint, "DEVICE_DEACTIVATED");
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

        auditLogger.logSecurityEvent(userId, null, "ALL_DEVICES_REVOKED");
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

    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((input + fingerprintSalt).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating device fingerprint hash", e);
            throw new DeviceFingerprintGenerationException("Failed to generate fingerprint hash", e);
        }
    }

    @Override
    public void deactivateDevice(String userId, String fingerprint) {
        userDeviceFingerprintRepository.findByUserIdAndFingerprint(Long.valueOf(userId), fingerprint)
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
        auditLogger.logSecurityEvent(String.valueOf(user.getUserId()), null, "DEVICE_FINGERPRINTING_DISABLED");
        log.warn("Device fingerprinting disabled for user: {}", user.getUserId());
    }

    @Override
    public void enableDeviceFingerprinting(User user) {
        user.setDeviceFingerprintingEnabled(true);
        userRepository.save(user);
        auditLogger.logSecurityEvent(String.valueOf(user.getUserId()), null, "DEVICE_FINGERPRINTING_ENABLED");
        log.warn("Device fingerprinting enabled for user: {}", user.getUserId());
    }
}