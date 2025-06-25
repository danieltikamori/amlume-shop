/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.authserver.security.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import me.amlu.authserver.common.StringUtils;
import me.amlu.authserver.exceptions.*;
import me.amlu.authserver.resilience.ratelimiter.RateLimiter;
import me.amlu.authserver.security.config.properties.SecurityProperties;
import me.amlu.authserver.security.enums.RiskLevel;
import me.amlu.authserver.security.model.DeviceFingerprintingInfo;
import me.amlu.authserver.security.model.UserDeviceFingerprint;
import me.amlu.authserver.security.repository.UserDeviceFingerprintRepository;
import me.amlu.authserver.user.dto.UserDeviceResponse;
import me.amlu.authserver.user.model.User;
import me.amlu.authserver.user.repository.UserRepository;
import me.amlu.authserver.user.dto.UserDeviceResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static me.amlu.authserver.common.Constants.MAX_FAILED_ATTEMPTS;
import static me.amlu.authserver.common.ErrorCodes.USER_NOT_FOUND;
import static me.amlu.authserver.common.HeaderNames.USER_AGENT;
import static me.amlu.authserver.common.HeaderNames.X_FORWARDED_FOR;
import static me.amlu.authserver.common.IpUtils.IP_HEADERS;
import static me.amlu.authserver.common.SecurityConstants.HASH_ALGORITHM;
import static me.amlu.authserver.common.SecurityConstants.INITIAL_MAP_CAPACITY;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;

@Service
@Transactional
public class DeviceFingerprintServiceImpl implements DeviceFingerprintService {

    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprintServiceImpl.class);

    private final UserRepository userRepository;
    private final UserDeviceFingerprintRepository userDeviceFingerprintRepository;
    private final AuditLogger auditLogger;
    private final SecurityProperties securityProperties;
    private final IpValidationService ipValidationService;
    private final IpSecurityService ipSecurityService;
    private final RateLimiter rateLimiter;
    private final GeoIp2Service geoIp2Service;
    private final AdvancedGeoService advancedGeoService;

    @Value("${security.device-fingerprint.fingerprint-salt}")
    private final String fingerprintSalt;

    @Value("${security.max-devices-per-user}")
    private final int maxDevicesPerUser;

    public DeviceFingerprintServiceImpl(@Value("${security.device-fingerprint.fingerprint-salt}") String fingerprintSalt,
                                        @Value("${security.max-devices-per-user}") int maxDevicesPerUser,
                                        UserRepository userRepository,
                                        UserDeviceFingerprintRepository userDeviceFingerprintRepository,
                                        AuditLogger auditLogger,
                                        SecurityProperties securityProperties,
                                        IpValidationService ipValidationService,
                                        IpSecurityService ipSecurityService,
                                        @Qualifier("redisSlidingWindowRateLimiter") RateLimiter rateLimiter, GeoIp2Service geoIp2Service, AdvancedGeoService advancedGeoService) {
        this.fingerprintSalt = Objects.requireNonNull(fingerprintSalt, "Fingerprint salt cannot be null");
        this.maxDevicesPerUser = maxDevicesPerUser;
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
        this.userDeviceFingerprintRepository = Objects.requireNonNull(userDeviceFingerprintRepository, "UserDeviceFingerprintRepository cannot be null");
        this.auditLogger = Objects.requireNonNull(auditLogger, "AuditLogger cannot be null");
        this.securityProperties = Objects.requireNonNull(securityProperties, "SecurityProperties cannot be null");
        this.ipValidationService = Objects.requireNonNull(ipValidationService, "IpValidationService cannot be null");
        this.ipSecurityService = Objects.requireNonNull(ipSecurityService, "IpSecurityService cannot be null");
        this.rateLimiter = rateLimiter;
        this.geoIp2Service = geoIp2Service;
        this.advancedGeoService = advancedGeoService;
    }

    // --- PRIMARY FINGERPRINT GENERATION METHOD ---

    /**
     * Generates a stable device fingerprint based on various attributes from the incoming HTTP request.
     * This method is the primary entry point for creating a device identifier.
     *
     * @param request The incoming HttpServletRequest.
     * @return A hashed, Base64URL-encoded string representing the device fingerprint.
     */
    @Override
    public String generateDeviceFingerprint(HttpServletRequest request) {
        try {
            // 1. Collect various data points from the request.
            Map<String, String> data = collectDeviceData(request);

            // 2. If no meaningful data was collected, return a fallback fingerprint.
            if (data.isEmpty()) {
                log.warn("No valid device data collected for fingerprinting. Using fallback.");
                return generateFallbackFingerprint();
            }

            // 3. Generate a stable hash from the collected data.
            return generateFingerprintHash(data);

        } catch (Exception e) {
            log.error("An unexpected error occurred while generating device fingerprint", e);
            return generateFallbackFingerprint(); // Fallback on any error.
        }
    }

    // --- HELPER METHODS FOR FINGERPRINT GENERATION ---

    /**
     * Gathers various identifying attributes from the HttpServletRequest.
     *
     * @param request The incoming request.
     * @return A map of collected data points.
     */
    private Map<String, String> collectDeviceData(HttpServletRequest request) {
        Map<String, String> data = new LinkedHashMap<>(INITIAL_MAP_CAPACITY);
        String userAgent = request.getHeader(USER_AGENT);

        // Core components for the fingerprint
        addDataIfValid(data, "IP-Address", getClientIpAddress(request));
        addDataIfValid(data, USER_AGENT, userAgent);
        addDataIfValid(data, ACCEPT_LANGUAGE, request.getHeader(ACCEPT_LANGUAGE));
        addDataIfValid(data, "Platform", getPlatform(userAgent)); // Derived from User-Agent

        // Additional headers for more entropy and stability
        addBrowserHeaders(data, request);
        addSecurityHeaders(data, request);

        return data;
    }

    /**
     * Hashes the collected device data to create a final, stable fingerprint string.
     *
     * @param data A map of device data.
     * @return A hashed and encoded fingerprint string.
     */
    private String generateFingerprintHash(Map<String, String> data) throws NoSuchAlgorithmException {
        // Use a TreeMap to ensure the keys are always sorted alphabetically.
        // This guarantees that the concatenated string is always the same for the same input data,
        // which is critical for a stable fingerprint.
        TreeMap<String, String> sortedData = new TreeMap<>(data);
        String rawFingerprint = buildRawFingerprint(sortedData);

        // The daily rotating time component was removed to ensure the fingerprint
        // remains stable for a device over time, which is better for UX.
        // If you need fingerprints to expire, it's better to manage that via the
        // lastUsedAt timestamp in the UserDeviceFingerprint entity.

        return hashWithSalt(rawFingerprint);
    }

    /**
     * Concatenates the sorted map of device data into a single string.
     *
     * @param sortedData The sorted map of data.
     * @return A single, delimited string of key-value pairs.
     */
    private String buildRawFingerprint(TreeMap<String, String> sortedData) {
        return sortedData.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining("|"));
    }

    /**
     * Hashes the input string using the configured algorithm and salt.
     *
     * @param input The string to hash.
     * @return A Base64URL-encoded hash string.
     */
    private String hashWithSalt(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] saltedInput = (input + fingerprintSalt).getBytes(StandardCharsets.UTF_8);
        byte[] hash = digest.digest(saltedInput);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    /**
     * Generates a random, non-identifying fingerprint as a fallback.
     */
    private String generateFallbackFingerprint() {
        return "fallback_" + UUID.randomUUID();
    }

    // --- Simplified helper methods ---

    private void addDataIfValid(Map<String, String> data, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            data.put(key, value);
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

    private String getPlatform(String userAgent) {
        if (!StringUtils.isNotBlank(userAgent)) {
            return "Unknown";
        }
        String uaLower = userAgent.toLowerCase(Locale.ENGLISH);
        if (uaLower.contains("windows")) return "Windows";
        if (uaLower.contains("mac")) return "macOS";
        if (uaLower.contains("linux")) return "Linux";
        if (uaLower.contains("android")) return "Android";
        if (uaLower.contains("ios") || uaLower.contains("iphone") || uaLower.contains("ipad")) return "iOS";
        return "Other";
    }

    private void addBrowserHeaders(Map<String, String> data, HttpServletRequest request) {
        addDataIfValid(data, "Accept", request.getHeader("Accept"));
        addDataIfValid(data, "Accept-Encoding", request.getHeader("Accept-Encoding"));
    }

    private void addSecurityHeaders(Map<String, String> data, HttpServletRequest request) {
        addDataIfValid(data, "Sec-Fetch-Site", request.getHeader("Sec-Fetch-Site"));
        addDataIfValid(data, "Sec-Fetch-Mode", request.getHeader("Sec-Fetch-Mode"));
        addDataIfValid(data, "Sec-Ch-Ua-Platform", request.getHeader("Sec-Ch-Ua-Platform"));
    }

    // --- OTHER SERVICE METHODS ---

    @Override
    public void registerDeviceFingerprint(String userId, String userAgent,
                                          String screenWidth, String screenHeight,
                                          HttpServletRequest request) throws DeviceFingerprintRegistrationException {
        // This method is now less ideal. The controller should call the new generateDeviceFingerprint method.
        // For now, we can adapt it to call the new method.
        validateInputs(userId, request);
        String clientIp = getClientIpAddress(request);

        // --- Apply rate limiting ---
        applyRateLimitingOnRegisterDevice(userId, clientIp);

        // --- Validate IP and spoofing ---
        ipSecurityCheckAtDeviceRegistration(userId, request, clientIp);

        logIpAddressResolution(userId, clientIp, request.getHeader(X_FORWARDED_FOR));

        try {
            User user = findAndValidateUser(userId);

            // Check if user has disabled device fingerprinting
            isDeviceFingerprintingEnabled(user);

            // Check if user has reached maximum devices
            validateDeviceLimit(user);

            String fingerprint = generateDeviceFingerprint(request); // Generate fingerprint

            // Extract metadata for the new fingerprint record
            String browserInfo = request.getHeader(USER_AGENT);
            String location = null; // Geo-location lookup would be here or in service
            String lastKnownCountry = null; // Geo-location lookup would be here or in service
            String deviceName = getDeviceNameFromUserAgent(browserInfo);
            String source = "Manual Registration";

            // Delegate to the service's storeOrUpdateFingerprint method
            // This method will handle finding/updating/creating the UserDeviceFingerprint entity
            storeOrUpdateFingerprint(user, fingerprint, browserInfo, clientIp, location, lastKnownCountry, deviceName, source);

        } catch (Exception e) {
            handleRegistrationError(userId, e);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserDeviceResponse> listUserDevices(Long userId, String currentDeviceFingerprint) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        List<UserDeviceFingerprint> fingerprints = userDeviceFingerprintRepository.findByUser(user);

        return fingerprints.stream()
                .map(fp -> UserDeviceResponse.builder() // Use the builder for clarity
                        .id(fp.getUserDeviceFingerprintId())
                        .deviceName(fp.getDeviceName())
                        .deviceFingerprint(fp.getDeviceFingerprint())
                        .browserInfo(fp.getBrowserInfo())
                        .lastKnownIp(fp.getLastKnownIp())
                        .location(fp.getLocation())
                        .lastKnownCountry(fp.getLastKnownCountry())
                        .lastUsedAt(fp.getLastUsedAt())
                        .active(fp.isActive())
                        .trusted(fp.isTrusted())
                        // Compare the stored fingerprint with the current request's fingerprint
                        .currentDevice(fp.getDeviceFingerprint().equals(currentDeviceFingerprint))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void revokeDevice(Long userId, String fingerprintId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        UserDeviceFingerprint fingerprint = userDeviceFingerprintRepository.findById(Long.valueOf(fingerprintId))
                .orElseThrow(() -> new DeviceFingerprintNotFoundException("Device fingerprint not found with ID: " + fingerprintId));

        if (!fingerprint.getUser().getId().equals(userId)) {
            throw new SecurityException("User not authorized to revoke this device fingerprint.");
        }

        fingerprint.setActive(false);
        fingerprint.setDeactivatedAt(Instant.now());
        userDeviceFingerprintRepository.save(fingerprint);
        log.info("Device fingerprint {} deactivated for user {}", fingerprintId, userId);
    }

    private void validateInputs(String userId, HttpServletRequest request) {
        if (StringUtils.isBlank(userId)) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
        Objects.requireNonNull(request, "Request cannot be null");
    }

    private void applyRateLimitingOnRegisterDevice(String userId, String clientIp) throws DeviceFingerprintRegistrationException {
        String rateLimitKey = "deviceFingerprintRegister:" + clientIp; // Key by IP for registration attempts
        try {
            if (!rateLimiter.tryAcquire(rateLimitKey)) {
                log.warn("Rate limit exceeded for IP: {} on device registration", clientIp);
                auditLogger.logSecurityEvent("RATE_LIMIT_EXCEEDED", userId, "Device Registration", clientIp);
                throw new DeviceFingerprintRegistrationException("Rate limit exceeded for device registration");
            }
        } catch (RateLimitException e) { // Catch Redis failure if fail-closed
            log.error("Rate limiting check failed for IP: {} on device registration. Denying request.", clientIp, e);
            throw new DeviceFingerprintRegistrationException("Rate limiter unavailable.", e);
        } catch (Exception e) { // Catch other unexpected errors
            log.error("Unexpected error during rate limit check for IP: {} on device registration", clientIp, e);
            throw new DeviceFingerprintRegistrationException("Error during rate limit check.", e);
        }
    }

    private void ipSecurityCheckAtDeviceRegistration(String userId, HttpServletRequest request, String clientIp) {

        // Check if IP is blocked
        if (ipSecurityService.isIpBlocked(clientIp)) {
            log.warn("Blocked IP address attempted device registration: {}", clientIp);
            auditLogger.logSecurityEvent("BLOCKED_IP_ATTEMPT", userId, clientIp);
            throw new BlockedIpAddressException("IP address is blocked");
        }
//        // Validate IP
//        if (!ipValidationService.isValidIp(clientIp)) {
//            log.warn("Invalid IP detected: {}", clientIp);
//            auditLogger.logSecurityEvent("INVALID_IP_ATTEMPT", userId, clientIp);
//        }
//
//        // Check for spoofing
//        if (ipSecurityService.isIpSuspicious(clientIp, request)) {
//            log.warn("Suspicious IP detected: {}", clientIp);
//            throw new SecurityException("Suspicious IP activity detected");
//        }
    }

    private User findAndValidateUser(String userId) {
        return userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));
    }

    private void isDeviceFingerprintingEnabled(User user) {
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

        // --- Check rate limiting ---
        rateLimiter.tryAcquire(clientIp);

        try {
            User user = findAndValidateUser(userId);
            UserDeviceFingerprint fingerprint = findAndValidateFingerprint(user, deviceFingerprint);

            validateIpSecurityAtExistingDeviceFingerprintValidation(userId, clientIp, fingerprint, request);
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
        // This method is now redundant as the primary generateDeviceFingerprint(HttpServletRequest)
        // extracts all necessary data. This method can be removed or marked @Deprecated.
        // For now, it will call the primary method.
        return generateDeviceFingerprint(request);
    }

    // @Deprecated in favor of a more simple approach
//    @Override
//    public String generateDeviceFingerprint(HttpServletRequest request) {
//        try {
//            Map<String, String> data = collectDeviceData(request);
//
//            // Browser capabilities and security headers
//            addBrowserHeaders(data, request);
//            addSecurityHeaders(data, request);
//
//            // Platform-specific information
//            addPlatformInfo(data, request);
//
//            if (data.isEmpty()) {
//                log.warn("No valid device data collected for fingerprinting");
//                return generateFallbackFingerprint();
//            }
//
//            return generateFingerprintHash(data);
//
//        } catch (Exception e) {
//            log.error("Error generating device fingerprint", e);
//            return generateFallbackFingerprint();
//        }
//    }

    private Map<String, String> collectDeviceData(String userAgent, String screenWidth,
                                                  String screenHeight, HttpServletRequest request) {
        // This method is also redundant if generateDeviceFingerprint(HttpServletRequest) is the primary.
        // It's kept for context but its usage should be phased out.
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

    // @Deprecated in favor of a more simple approach
//    private String getPlatform(String userAgent) {
//        if (StringUtils.isBlank(userAgent)) {
//            return null;
//        }
//
//        // Convert to lowercase once and store
//        String userAgentLower = userAgent.toLowerCase(Locale.ENGLISH);
//
//        // Use switch for better performance with string matching
//        if (userAgentLower.contains("windows")) {
//            return "Windows";
//        }
//
//        if (userAgentLower.contains("mac")) {  // Simplified Mac detection
//            return "macOS";
//        }
//
//        if (userAgentLower.contains("linux")) {
//            return "Linux";
//        }
//
//        if (userAgentLower.contains("android")) {
//            return "Android";
//        }
//
//        if (userAgentLower.contains("ios")) {
//            return "iOS";
//        }
//
//        return "Other"; // Or handle unknown platforms differently
//    }

    // --- Comprehensive methods - For now, we'll utilize a more compact approach ---
//    private void addBrowserHeaders(Map<String, String> data, HttpServletRequest request) {
//        addDataIfValid(data, "Accept", request.getHeader("Accept"));
//        addDataIfValid(data, "Accept-Encoding", request.getHeader("Accept-Encoding"));
//        addDataIfValid(data, "Connection", request.getHeader("Connection"));
//        addDataIfValid(data, "Host", request.getHeader("Host"));
//    }
//
//    private void addSecurityHeaders(Map<String, String> data, HttpServletRequest request) {
//        addDataIfValid(data, "Sec-Fetch-Site", request.getHeader("Sec-Fetch-Site"));
//        addDataIfValid(data, "Sec-Fetch-Mode", request.getHeader("Sec-Fetch-Mode"));
//        addDataIfValid(data, "Sec-Fetch-User", request.getHeader("Sec-Fetch-User"));
//        addDataIfValid(data, "Sec-Fetch-Dest", request.getHeader("Sec-Fetch-Dest"));
//        addDataIfValid(data, "Sec-Ch-Ua", request.getHeader("Sec-Ch-Ua"));
//        addDataIfValid(data, "Sec-Ch-Ua-Mobile", request.getHeader("Sec-Ch-Ua-Mobile"));
//        addDataIfValid(data, "Sec-Ch-Ua-Platform", request.getHeader("Sec-Ch-Ua-Platform"));
//    }

    private void addPlatformInfo(Map<String, String> data, HttpServletRequest request) {
        // Add any custom headers the frontend sends
        addDataIfValid(data, "Platform-Type", request.getHeader("X-Platform-Type"));
        addDataIfValid(data, "App-Version", request.getHeader("X-App-Version"));
        addDataIfValid(data, "Device-Model", request.getHeader("X-Device-Model"));
    }

    private void addSessionData(Map<String, String> data, HttpServletRequest request) {
        Optional.ofNullable(request.getSession(false))
                .map(HttpSession::getId)
                .ifPresent(sessionId -> addDataIfValid(data, "Session-ID", sessionId));
    }

//    /**
//     * Deprecated in favor to a better UX implementation
//     *  Add time stamp to fingerprint
//     * @param data map of request data
//     * @return
//     */
//    private String generateFingerprintHash(Map<String, String> data) {
//        try {
//            TreeMap<String, String> sortedData = new TreeMap<>(data);
//            StringBuilder rawFingerprint = new StringBuilder(buildRawFingerprint(sortedData));
//
//            // Add timestamp-based component for uniqueness
//            String timeComponent = String.valueOf(System.currentTimeMillis() / (24 * 60 * 60 * 1000)); // Daily rotation
//            rawFingerprint.append(timeComponent);
//
//            return hashWithSalt(rawFingerprint.toString());
//        } catch (NoSuchAlgorithmException e) {
//            log.error("Hash algorithm not available", e);
//            return generateFallbackFingerprint();
//        }
//    }

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

    private void validateIpSecurityAtExistingDeviceFingerprintValidation(String userId, String clientIp, UserDeviceFingerprint fingerprint, HttpServletRequest request) {
        try {
            // Check if IP is blocked
            if (ipSecurityService.isIpBlocked(clientIp)) {
                log.warn("Blocked IP address attempted access: {}", clientIp);
                auditLogger.logSecurityEvent("BLOCKED_IP_ATTEMPT", userId, String.valueOf(fingerprint.getUser().getUserId()), clientIp);
                throw new BlockedIpAddressException("IP address is blocked");
            }

            // Check for spoofing
            if (ipSecurityService.isIpSuspicious(clientIp, request)) {
                log.warn("Suspicious IP detected at existing fingerprint validation: {}", clientIp);
                throw new SecurityException("Suspicious IP activity detected");
            }

            // Validate IP format
            if (!ipValidationService.isValidIp(clientIp)) {
                log.warn("Invalid IP address detected: {}", clientIp);
                auditLogger.logSecurityEvent("INVALID_IP_ATTEMPT", userId, String.valueOf(fingerprint.getUserDeviceFingerprintId()), clientIp);
                throw new InvalidIpAddressException("Invalid IP address format");
            }


            // Log successful validation
            log.debug("IP security validation passed for IP: {}", clientIp);

        } catch (RateLimitExceededException | TooManyAttemptsException e) { // Catch the specific exception(s)
            // Handle rate limit exceedance specifically
            log.warn("Rate limit exceeded for IP: {} on fingerprint validation: {}", clientIp, e.getMessage());
            auditLogger.logSecurityEvent("RATE_LIMIT_EXCEEDED", userId, String.valueOf(fingerprint.getUser().getUserId()), clientIp);
            // Re-throw the original exception as the calling method might expect it
            throw e;
        } catch (InvalidIpAddressException | BlockedIpAddressException e) {
            // Re-throw specific IP validation exceptions
            throw e;
        } catch (Exception e) {
            // Catch any other unexpected errors
            log.error("Unexpected error during IP security validation for IP {}: {}", clientIp, e.getMessage(), e);
            throw new IpValidationException("Error during IP security validation", e);
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
        String currentFingerprint = generateDeviceFingerprint(request);

        // Extract metadata from the request for comparison or logging
        String browserInfo = request.getHeader(USER_AGENT);
        String lastKnownIp = getClientIpAddress(request);
        String location = null;
        String lastKnownCountry = null;
        String deviceName = getDeviceNameFromUserAgent(browserInfo);
        String source = "Verification Event";

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
                    .ifPresent(this::updateExistingFingerprint); // Call the public update method
        }

        log.info("Device fingerprint verified for user: {}", userId);
    }

    private boolean isValidFingerprintFormat(String fingerprint) {
        // Implement the fingerprint validation logic.
        // For example, checking length, allowed characters, etc.
        return fingerprint.matches("^[a-zA-Z0-9-_]{32,256}$");
    }

    @Override
    public void trustDevice(long userId, String deviceFingerprint) {
        userDeviceFingerprintRepository.findByUserIdAndDeviceFingerprint(userId, deviceFingerprint)
                .ifPresent(device -> {
                    device.setTrusted(true);
                    device.setLastUsedAt(Instant.now());
                    userDeviceFingerprintRepository.save(device);
                });
    }


    @Override
    public void storeOrUpdateFingerprint(User user, String deviceFingerprint,
                                         String browserInfo, String lastKnownIp, String location,
                                         String lastKnownCountry, String deviceName, String source) {
        validateInput(user, deviceFingerprint);

        userDeviceFingerprintRepository.findByUserAndDeviceFingerprint(user, deviceFingerprint)
                .ifPresentOrElse(
                        this::updateExistingFingerprint,
                        () -> createNewFingerprint(user, deviceFingerprint,
                                browserInfo, lastKnownIp, location, lastKnownCountry, deviceName, source)
                );
    }

    @Override
    public void updateExistingFingerprint(UserDeviceFingerprint fingerprint) {
        fingerprint.setLastUsedAt(Instant.now());
        fingerprint.setFailedAttempts(0); // Reset failed attempts on successful update
        userDeviceFingerprintRepository.save(fingerprint);

        auditLogger.logDeviceAccess(String.valueOf(fingerprint.getUser().getUserId()),
                fingerprint.getDeviceFingerprint(),
                "DEVICE_UPDATED");
    }

    @Override
    public void createNewFingerprint(User user, String deviceFingerprint,
                                     String browserInfo, String lastKnownIp, String location,
                                     String lastKnownCountry, String deviceName, String source) {
        // Check device limit
        checkDeviceLimit(user);

        UserDeviceFingerprint newFingerprint = UserDeviceFingerprint.builder()
                .user(user)
                .deviceFingerprint(deviceFingerprint)
                .lastUsedAt(Instant.now())
                .active(true)
                .trusted(false)
                .failedAttempts(0)
                .browserInfo(browserInfo)
                .lastKnownIp(lastKnownIp)
                .location(location)
                .lastKnownCountry(lastKnownCountry)
                .deviceName(deviceName)
                .source(source)
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

    public void enrichDeviceFingerprintWithGeoData(UserDeviceFingerprint fingerprint, String clientIp, User user) {
        // 1. Get detailed geolocation information
        geoIp2Service.lookupLocation(clientIp).ifPresent(geoLocation -> {
            // Populate fields in UserDeviceFingerprint from GeoLocation
            fingerprint.setLocation(geoLocation.getCity());
            fingerprint.setLastKnownCountry(geoLocation.getCountryCode());
            // You could also store latitude, longitude, timeZone, etc., if your entity supports it
            // fingerprint.setLatitude(geoLocation.getLatitude());
            // fingerprint.setLongitude(geoLocation.getLongitude());
            // fingerprint.setTimeZone(geoLocation.getTimeZone());
        });

        // 2. Perform advanced geo-security checks
        // The userId is important for impossible travel detection (to retrieve historical locations)
        GeoVerificationResult geoResult = advancedGeoService.verifyLocation(clientIp, user.getId().toString());

        // Based on the risk level, you can decide to mark the device as not trusted
        if (geoResult.getRisk() == RiskLevel.HIGH) {
            fingerprint.setTrusted(false); // Mark as untrusted due to high risk
            // Log the alerts for auditing/monitoring
            log.warn("High risk geo-location detected for user {}: IP={}, Alerts={}",
                    user.getEmail().getValue(), clientIp, geoResult.getAlerts());
        } else if (geoResult.getRisk() == RiskLevel.MEDIUM) {
            // For medium risk, you might still set trusted to false or just log a warning
            fingerprint.setTrusted(false); // Or keep it true, depending on your policy
            log.info("Medium risk geo-location detected for user {}: IP={}, Alerts={}",
                    user.getEmail().getValue(), clientIp, geoResult.getAlerts());
        } else {
            // If risk is LOW, you might default to trusted (assuming other factors don't override)
            // fingerprint.setTrusted(true); // This would be the default if not set otherwise
        }

        // You could also store the raw alerts or risk level in a dedicated field in UserDeviceFingerprint
        // if you need to persist the reason for the risk assessment.
        // Example: fingerprint.setRiskLevel(geoResult.getRisk().name());
        // Example: fingerprint.setGeoAlerts(String.join("; ", geoResult.getAlerts()));
    }

    @Override
    public void markDeviceSuspicious(String userId, String deviceFingerprint) {
        userDeviceFingerprintRepository.findByUserIdAndDeviceFingerprint(Long.valueOf(userId), deviceFingerprint)
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
        return userDeviceFingerprintRepository.existsByUserAndDeviceFingerprint(user, fingerprint);
    }

    @Override
    public void revokeAllDevices(String userId, String exceptFingerprint) {
        List<UserDeviceFingerprint> devices = userDeviceFingerprintRepository
                .findByUserIdAndDeviceFingerprintNot(Long.valueOf(userId), exceptFingerprint);

        devices.forEach(device -> {
            device.setActive(false);
            device.setDeactivatedAt(Instant.now());
            userDeviceFingerprintRepository.save(device);
        });

        auditLogger.logSecurityEvent("ALL_DEVICES_REVOKED EXCEPT this fingerprint:", userId, exceptFingerprint);
    }

    @Override
    public void deactivateDevice(String userId, String deviceFingerprint) {
        userDeviceFingerprintRepository.findByUserIdAndDeviceFingerprint(Long.valueOf(userId), deviceFingerprint)
                .ifPresent(device -> {
                    device.setActive(false);
                    device.setDeactivatedAt(Instant.now());
                    userDeviceFingerprintRepository.save(device);
                });
    }

    @Override
    @Transactional
    public void disableDeviceFingerprinting(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        // 1. Create the new disabled state for DeviceFingerprintingInfo
        // Use the existing method on the embeddable which returns a new instance
        DeviceFingerprintingInfo currentInfo = user.getDeviceFingerprintingInfo();
        DeviceFingerprintingInfo disabledInfo;
        if (currentInfo != null) {
            disabledInfo = currentInfo.disableFingerprinting(); // Creates new instance with enabled=false, fingerprints=null
        } else {
            // Handle case where info might be null (though unlikely with @Embedded)
            disabledInfo = DeviceFingerprintingInfo.builder()
                    .deviceFingerprintingEnabled(false)
                    .build();
        }

        // 2. Update the User entity with the new immutable embeddable
        user.updateDeviceFingerprintingInfo(disabledInfo);
        userRepository.save(user); // Save the user with the updated embedded object

        // 3. Find and deactivate associated *active* device fingerprints
        List<UserDeviceFingerprint> activeDevices = userDeviceFingerprintRepository.findByUserAndActiveTrue(user);
        if (!activeDevices.isEmpty()) {
            Instant now = Instant.now();
            for (UserDeviceFingerprint device : activeDevices) {
                device.setActive(false);
                device.setDeactivatedAt(now);
                // Don't save inside the loop for efficiency
            }
            userDeviceFingerprintRepository.saveAll(activeDevices); // Save all changes at once
            log.info("Deactivated {} active device fingerprints for user: {}", activeDevices.size(), user.getUserId());
        }

        // 4. Logging
        auditLogger.logSecurityEvent("DEVICE_FINGERPRINTING_DISABLED", String.valueOf(user.getUserId()), null);
        log.info("Device fingerprinting disabled for user: {}", user.getUserId()); // Use INFO level
    }

    @Override
    @Transactional
    public void enableDeviceFingerprinting(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        // 1. Get current info (handle potential null)
        DeviceFingerprintingInfo currentInfo = user.getDeviceFingerprintingInfo();
        DeviceFingerprintingInfo enabledInfo;

        if (currentInfo != null) {
            // Use the existing method which returns a new instance, preserving existing fingerprints
            enabledInfo = currentInfo.enableFingerprinting();
        } else {
            // If it was somehow null, create a new enabled one from scratch
            enabledInfo = DeviceFingerprintingInfo.builder()
                    .deviceFingerprintingEnabled(true)
                    // No existing fingerprints to preserve if currentInfo was null
                    .build();
        }

        // 2. Update the User entity with the new immutable embeddable
        user.updateDeviceFingerprintingInfo(enabledInfo);
        userRepository.save(user); // Save the user with the updated embedded object

        // 3. Logging
        // Note: We don't automatically reactivate devices here. They remain inactive.
        auditLogger.logSecurityEvent("DEVICE_FINGERPRINTING_ENABLED", String.valueOf(user.getUserId()), null);
        log.info("Device fingerprinting enabled for user: {}", user.getUserId()); // Use INFO level
    }

    @Override
    public List<UserDeviceFingerprint> getUserDevices(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        return userDeviceFingerprintRepository.findByUser(user);
    }

    @Override
    public UserDeviceFingerprint getDeviceById(Long deviceId) {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID cannot be null");
        }

        return userDeviceFingerprintRepository.findById(deviceId).orElse(null);
    }

    @Override
    public void untrustDevice(long userId, String fingerprint) {
        userDeviceFingerprintRepository.findByUserIdAndDeviceFingerprint(userId, fingerprint)
                .ifPresent(device -> {
                    device.setTrusted(false);
                    device.setLastUsedAt(Instant.now());
                    userDeviceFingerprintRepository.save(device);

                    auditLogger.logDeviceAccess(String.valueOf(userId),
                            fingerprint,
                            "DEVICE_UNTRUSTED");
                });
    }

    /**
     * Extracts a human-readable device name from a User-Agent string.
     *
     * @param userAgent The User-Agent string from the HTTP request.
     * @return A string representing the device name (e.g., "iPhone", "Windows PC").
     */
    @Override // Now implements the interface method
    public String getDeviceNameFromUserAgent(String userAgent) {
        if (!StringUtils.isNotBlank(userAgent)) {
            return "Unknown Device";
        }
        String uaLower = userAgent.toLowerCase(Locale.ENGLISH);
        if (uaLower.contains("mobile") || uaLower.contains("android") || uaLower.contains("iphone") || uaLower.contains("ipad")) {
            if (uaLower.contains("iphone")) return "iPhone";
            if (uaLower.contains("ipad")) return "iPad";
            if (uaLower.contains("android")) return "Android Device";
            return "Mobile Device";
        }
        if (uaLower.contains("windows")) return "Windows PC";
        if (uaLower.contains("mac")) return "Mac";
        if (uaLower.contains("linux")) return "Linux PC";
        return "Desktop PC"; // Generic fallback
    }
}
